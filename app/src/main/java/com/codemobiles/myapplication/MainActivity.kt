package com.codemobiles.myapplication

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var remoteModel // For loading the model remotely
            : FirebaseAutoMLRemoteModel? = null
    var labeler //For running the image labeler
            : FirebaseVisionImageLabeler? = null
    var optionsBuilder // Which option is use to run the labeler local or remotely
            : FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder? = null
    var progressDialog //Show the progress dialog while model is downloading...
            : ProgressDialog? = null
    var conditions //Conditions to download the model
            : FirebaseModelDownloadConditions? = null
    var image // preparing the input image
            : FirebaseVisionImage? = null
    var textView // Displaying the label for the input image
            : TextView? = null
    var button // To select the image from device
            : Button? = null
    var imageView //To display the selected image
            : ImageView? = null
    private var localModel: FirebaseAutoMLLocalModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.text)
        button = findViewById(R.id.selectImage)
        imageView = findViewById(R.id.image)

        selectImage.setOnClickListener(View.OnClickListener {
            CropImage.activity().start(this@MainActivity)
            //fromRemoteModel()
        })
    }
    private fun setLabelerFromLocalModel(uri: Uri) {
        localModel = FirebaseAutoMLLocalModel.Builder()
            .setAssetFilePath("model/manifest.json")
            .build()
        try {
            val options =
                FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel!!)
                    .setConfidenceThreshold(0.0f)
                    .build()
            labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options)
            image = FirebaseVisionImage.fromFilePath(this@MainActivity, uri)
            processImageLabeler(labeler!!, image!!)
        } catch (e: FirebaseMLException) { // ...
        } catch (e: IOException) {
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                if (result != null) {
                    val uri = result.uri //path of image in phone
                    imageView!!.setImageURI(uri) //set image in imageview
                    textView!!.text = "" //so that previous text don't get append with new one
                    setLabelerFromLocalModel(uri)
                    //setLabelerFromRemoteLabel(uri);
                } else progressDialog!!.cancel()
            } else progressDialog!!.cancel()
        }
    }

    private fun setLabelerFromRemoteLabel(uri: Uri) {
        FirebaseModelManager.getInstance().isModelDownloaded(remoteModel!!)
            .addOnCompleteListener { task ->
                if (task.isComplete) {
                    optionsBuilder =
                        FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel!!)
                    val options =
                        optionsBuilder!!
                            .setConfidenceThreshold(0.0f)
                            .build()
                    try {
                        labeler =
                            FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options)
                        image = FirebaseVisionImage.fromFilePath(this@MainActivity, uri)
                        processImageLabeler(labeler!!, image!!)
                    } catch (exception: FirebaseMLException) {
                        Log.e("TAG", "onSuccess: $exception")
                        Toast.makeText(this@MainActivity, "Ml exeception", Toast.LENGTH_SHORT)
                            .show()
                    } catch (exception: IOException) {
                        Log.e("TAG", "onSuccess: $exception")
                        Toast.makeText(this@MainActivity, "Ml exeception", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else Toast.makeText(
                    this@MainActivity,
                    "Not downloaded",
                    Toast.LENGTH_SHORT
                ).show()
            }.addOnFailureListener { e ->
                Log.e("TAG", "onFailure: $e")
                Toast.makeText(this@MainActivity, "err$e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processImageLabeler(
        labeler: FirebaseVisionImageLabeler,
        image: FirebaseVisionImage
    ) {
        labeler.processImage(image).addOnCompleteListener { task ->
            progressDialog!!.cancel()
            for (label in task.result!!) {
                val eachlabel = label.text
                val confidence = label.confidence
                textView!!.append(
                    "$eachlabel - " + ("" + confidence * 100).subSequence(
                        0,
                        4
                    ) + "%" + "\n\n"
                )
            }
            //                Intent intent = new Intent();
            //                intent.setAction(Intent.ACTION_VIEW);
            //                intent.setData(Uri.parse("https://www.google.com/search?q=" + task.getResult().get(0).getText()));
            //                startActivity(intent);
        }.addOnFailureListener { e ->
            Log.e("OnFail", "" + e)
            Toast.makeText(this@MainActivity, "Something went wrong! $e", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun fromRemoteModel() {
        progressDialog!!.show() /* model name*/
        remoteModel = FirebaseAutoMLRemoteModel.Builder("SneakerRecog_2020420141351").build()
        conditions = FirebaseModelDownloadConditions.Builder().requireWifi().build()
        //first download the model
        FirebaseModelManager.getInstance().download(remoteModel!!, conditions!!)
            .addOnCompleteListener {
                CropImage.activity().start(this@MainActivity) // open image crop activity
            }
    }
}