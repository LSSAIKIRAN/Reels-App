package com.kiran.snapee.Fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.kiran.snapee.MainActivity
import com.kiran.snapee.ModelClasses.VideoModel
import com.kiran.snapee.databinding.FragmentUploadVideoBinding


class UploadVideoFragment : Fragment() {

    private lateinit var binding: FragmentUploadVideoBinding

    private var selectedVideoUri: Uri? = null
    lateinit var videoLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUploadVideoBinding.inflate(inflater, container, false)

        videoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    selectedVideoUri = result.data?.data
                    showPostView();
                }
            }
        binding.uploadView.setOnClickListener {
            checkPermissionAndOpenVideoPicker()
        }

        binding.Done.setOnClickListener {
            postVideo();
        }

        binding.cancelBtn.setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity::class.java))
            activity?.finish()
        }


        return binding.root
    }

    private fun postVideo() {
        if (binding.Caption.text.toString().isEmpty()) {
            binding.Caption.setError("Write something")
            return
        }
        setInProgress(true);
        selectedVideoUri?.apply {
            //store in firebase cloud storage

            val videoRef = FirebaseStorage.getInstance()
                .reference
                .child("videos/" + this.lastPathSegment)
            videoRef.putFile(this)
                .addOnSuccessListener {
                    videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        //video model store in firebase firestore
                        postToFirestore(downloadUrl.toString())
                    }
                }


        }

    }

    private fun postToFirestore(url: String) {
        val videoModel = VideoModel(
            FirebaseAuth.getInstance().currentUser?.uid!! + "_" + Timestamp.now().toString(),
            binding.Caption.text.toString(),
            url,
            FirebaseAuth.getInstance().currentUser?.uid!!,
            Timestamp.now(),
        )
        Firebase.firestore.collection("videos")
            .document(videoModel.videoId)
            .set(videoModel)
            .addOnSuccessListener {
                setInProgress(false);
                Toast.makeText(requireContext(), "Video uploaded", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                activity?.finish()
            }.addOnFailureListener {
                setInProgress(false)
                Toast.makeText(requireContext(), "Video failed", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                activity?.finish()
            }

    }


    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.Done.visibility = View.GONE
            Toast.makeText(requireContext(), "Please Wait...", Toast.LENGTH_SHORT).show()
            binding.PleaseWait.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.Done.visibility = View.VISIBLE
            binding.PleaseWait.visibility = View.GONE
        }
    }

    private fun showPostView() {
        selectedVideoUri?.let {
            binding.postView.visibility = View.VISIBLE
            binding.uploadView.visibility = View.GONE
            Glide.with(binding.Thumbnail).load(it).into(binding.Thumbnail)
        }

    }

    private fun checkPermissionAndOpenVideoPicker() {
        var readExternalVideo: String = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalVideo = android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            readExternalVideo = android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                readExternalVideo
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //we have permission
            openVideoPicker()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(readExternalVideo),
                100
            )
        }
    }

    private fun openVideoPicker() {
        var intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        videoLauncher.launch(intent)
    }
}
