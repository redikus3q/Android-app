package com.example.myapplication

import android.R.attr.bottom
import android.R.attr.left
import android.R.attr.right
import android.R.attr.top
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.ViewGroup.LayoutParams.FILL_PARENT
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.myapplication.databinding.ActivityPostListBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.FileOutputStream
import java.util.jar.Attributes

class CompStrings: Comparator<StorageReference>{
    override fun compare(o1: StorageReference, o2: StorageReference): Int {
        if(o1 == null || o2 == null)
            return 0

        return o1.toString().compareTo(o2.toString())
    }
}

class PostList : AppCompatActivity() {

    var user: String = ""
    var postCount: Int = 0
    lateinit var storage: FirebaseStorage

    private lateinit var binding: ActivityPostListBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_list)

        binding = ActivityPostListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Initiate storage
        storage = Firebase.storage
        val storageRef = storage.reference

        // Set user
        val extras = intent.extras
        if (extras != null) {
            user = extras.getString("user").toString()
        }

        // Download a photo
        val ONE_MEGABYTE: Long = 1024 * 1024
        val link = "gs://androidapp-1d5d7.appspot.com/$user/"
        val folderReference = storage.getReferenceFromUrl(link)
        folderReference.listAll().addOnSuccessListener { listResult ->
            val comparator = CompStrings()
            val boss = listResult.items.sortedWith(comparator)
            for (fileRef in boss) {
                val tempLink = link + fileRef.name
                val fileReference = storage.getReferenceFromUrl(tempLink)
                fileReference.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                    addImageView(it);
                }.addOnFailureListener {
                    Toast.makeText(this,  it.toString(), Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this,  it.toString(), Toast.LENGTH_LONG).show()
        }

        // Navigation
        val btmNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        btmNav.menu.getItem(1).isChecked = true
        btmNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.post -> {
                    Handler().post(Runnable {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("user", user)
                        startActivity(intent)
                        finish()
                    })
                }
                R.id.help -> {
                    Handler().post(Runnable {
                        val intent = Intent(this, Help::class.java)
                        intent.putExtra("user", user)
                        startActivity(intent)
                        finish()
                    })
                }
                else ->{
                }
            }
            true
        }
    }

    private fun addImageView(bytes: ByteArray) {
        val imageView = ImageView(this)
        val params = LinearLayout.LayoutParams(
            FILL_PARENT,
            800
        )
        params.setMargins(0, 50, 0, 50)
        imageView.layoutParams = params

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        imageView.setImageBitmap(bitmap)
        imageView.id = postCount

        // Add ImageView to LinearLayout
        binding.scrollableLayout.addView(imageView)

        val dynamicButton = Button(this)
        val paramsButton = LinearLayout.LayoutParams(
            FILL_PARENT,
            150
        )
        paramsButton.setMargins(120, -20, 120, 50)
        dynamicButton.layoutParams = paramsButton

        dynamicButton.text = "Share"
        dynamicButton.id = postCount
        postCount += 1
        binding.scrollableLayout.addView(dynamicButton)

        dynamicButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/jpeg"

            // Save the bitmap to a temporary file
            val cachePath = File(externalCacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_image.jpg")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            // Attach the photo to the intent
            val photoUri = FileProvider.getUriForFile(this, packageName + ".fileprovider", file)
            intent.putExtra(Intent.EXTRA_STREAM, photoUri)

            // Launch the sharing activity
            startActivity(Intent.createChooser(intent, "Share Photo"))
        }
    }



}