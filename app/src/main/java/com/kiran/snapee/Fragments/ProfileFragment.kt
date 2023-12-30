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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.kiran.snapee.ModelClasses.UserModel
import com.kiran.snapee.R
import com.kiran.snapee.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding


    private lateinit var currentUserId: String
    private lateinit var photoLauncher: ActivityResultLauncher<Intent>


    private lateinit var profileUserModel: UserModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)


        currentUserId =  FirebaseAuth.getInstance().currentUser?.uid!!

        photoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if(result.resultCode == AppCompatActivity.RESULT_OK){
                uploadToFirestore(result.data?.data!!)
            }
        }


        binding.profileImage.setOnClickListener {
            checkPermissionAndPickPhoto()
        }


        binding.followBtn.setOnClickListener {
            followUnfollowUser()
        }

        getProfileDataFromFirebase()

        return binding.root
    }

    private fun followUnfollowUser() {
        Firebase.firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener {
                val currentUserModel = it.toObject(UserModel::class.java)!!
                if(profileUserModel.followerList.contains(currentUserId)){
                    //unfollow user
                    profileUserModel.followerList.remove(currentUserId)
                   // currentUserModel.followingList.remove(profileUserId)
                    binding.followBtn.text = "Follow"
                }else{
                    //follow user
                    profileUserModel.followerList.add(currentUserId)
                   // currentUserModel.followingList.add(profileUserId)
                    binding.followBtn.text = "Unfollow"
                }
                updateUserData(profileUserModel)
                updateUserData(currentUserModel)


            }
    }

    private fun updateUserData(model :UserModel) {
        Firebase.firestore.collection("users")
            .document(model.id)
            .set(model)
            .addOnSuccessListener {
                getProfileDataFromFirebase()
            }
    }

    private fun checkPermissionAndPickPhoto() {
        var readExternalPhoto : String = ""
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            readExternalPhoto = android.Manifest.permission.READ_MEDIA_IMAGES
        }else{
            readExternalPhoto = android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if(ContextCompat.checkSelfPermission(requireContext(),readExternalPhoto)== PackageManager.PERMISSION_GRANTED){
            //we have permission
            openPhotoPicker()
        }else{
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(readExternalPhoto),
                100
            )
        }
    }

    private fun openPhotoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        photoLauncher.launch(intent)
    }

    private fun uploadToFirestore(photoUri : Uri) {
        binding.progressBar.visibility = View.VISIBLE
        val photoRef =  FirebaseStorage.getInstance()
            .reference
            .child("profilePic/"+ currentUserId )
        photoRef.putFile(photoUri)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener {downloadUrl->
                    //video model store in firebase firestore
                    postToFirestore(downloadUrl.toString())
                }
            }
    }

    private fun postToFirestore(url : String) {
        Firebase.firestore.collection("users")
            .document(currentUserId)
            .update("profilePic",url)
            .addOnSuccessListener {
                getProfileDataFromFirebase()
            }
    }

    private fun getProfileDataFromFirebase() {
        Firebase.firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener {
                profileUserModel = it.toObject(UserModel::class.java)!!
                setUI()
            }
    }

    private fun setUI() {
        profileUserModel.apply {
            Glide.with(binding.profileImage).load(profilePic)
                .apply(RequestOptions().placeholder(R.drawable.profilein))
                .circleCrop()
                .into(binding.profileImage)
            binding.profileUsername.text ="@"+ username
            if(profileUserModel.followerList.contains(currentUserId))
                binding.followBtn.text = "Unfollow"
            binding.progressBar.visibility = View.INVISIBLE
            binding.followingcount.text = followingList.size.toString()
            binding.followercount.text = followerList.size.toString()
            Firebase.firestore.collection("videos")
                .whereEqualTo("uploaderId",currentUserId)
                .get().addOnSuccessListener {
                    binding.postcount.text  = it.size().toString()
                }
        }
    }

}