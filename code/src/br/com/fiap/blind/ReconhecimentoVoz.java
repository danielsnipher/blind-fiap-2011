package br.com.fiap.blind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class ReconhecimentoVoz extends Activity implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener, Runnable {

	private ArrayList<String> lista;
	private static int VOICE_RECOGNITION_REQUEST_CODE = 1;
	private static int DESAMBIGUA_VOZ = 2;
	private TextToSpeech mTts;
	private boolean ttsInitialized;
	private ReentrantLock waitForInitLock = new ReentrantLock();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		setContentView(R.layout.main);

		Handler handler = new Handler();
		handler.postDelayed(this, 3000);
	}

	@Override
	protected void onDestroy()
	{
		// Don't forget to shutdown!
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}

		super.onDestroy();
	}

	@Override
	public void run() {
		ttsInitialized = false;
		mTts = new TextToSpeech(this, this);
		waitForInitLock.lock();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {

			// Fill the list view with the strings the recognizer thought it could have heard
			lista = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

			Intent itDesambigua = new Intent(this, DesambiguaVoz.class);
			itDesambigua.putExtra("lista", lista);
			startActivityForResult(itDesambigua, DESAMBIGUA_VOZ);

		}
		if (requestCode == DESAMBIGUA_VOZ) {

			if (resultCode == 1) {

				String endereco = data.getStringExtra("texto");
				Intent itEndereco = new Intent(this, Endereco.class);
				itEndereco.putExtra("endereco", endereco);
				startActivity(itEndereco);
				finish();
			}
		}
	}

	public void onInit(int status) {
		// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.

		if (status == TextToSpeech.SUCCESS) {

			//unlock it so that speech will happen 
			waitForInitLock.unlock(); 
			ttsInitialized = true;

			if (waitForInitLock.isLocked()) 
			{ 
				try 
				{ 
					waitForInitLock.tryLock(180, TimeUnit.SECONDS); 
				} 
				catch (InterruptedException e) 
				{ 
					Log.e("speaker", "interruped"); 
				} 
				//unlock it here so that it is never locked again 
				waitForInitLock.unlock(); 
			} 
			if (ttsInitialized) {
				int result = mTts.setOnUtteranceCompletedListener(this); 
				if (result == TextToSpeech.ERROR) 
				{ 
					Log.e("speaker", "failed to add utterance listener"); 
				} 

				HashMap<String, String> myHashText = new HashMap<String, String>();
				myHashText.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DigaEndereco");
				mTts.speak("Diga o endere�o de destino", TextToSpeech.QUEUE_FLUSH, myHashText);

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				Log.e("ReconhecimentoVoz", "TextToSpeech not initialized");
			}


		} else {
			// Initialization failed.
			Log.e("ReconhecimentoVoz", "Could not initialize TextToSpeech.");
		}
	}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		if (utteranceId.equals("DigaEndereco")) {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Diga o endere�o de destino");
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(1500);
			startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
		}
	}
}
