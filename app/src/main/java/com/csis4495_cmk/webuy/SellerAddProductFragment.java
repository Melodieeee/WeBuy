package com.csis4495_cmk.webuy;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.csis4495_cmk.webuy.adapters.SellerAddProductImagesAdapter;
import com.csis4495_cmk.webuy.adapters.SellerStyleListAdapter;
import com.csis4495_cmk.webuy.fragments.SellerAddStyleFragment;
import com.csis4495_cmk.webuy.models.Product;
import com.csis4495_cmk.webuy.models.ProductStyle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SellerAddProductFragment extends Fragment implements SellerAddProductImagesAdapter.onImgClick,
        SellerAddStyleFragment.onFragmentStyleListener,
        SellerStyleListAdapter.onItemClick {

    private final int REQUEST_PERMISSION_CODE = 10;
    private FirebaseAuth auth;
    private StorageReference productImgsReference;
    private DatabaseReference firebaseDatabase;
    private FirebaseDatabase firebaseInstance;
    private FirebaseUser firebaseUser;
    private ActivityResultLauncher<String> productImgFilePicker;
    private RecyclerView recyclerViewProductImgs;
    private List<Uri> uriUploadProductImgs;
    private RecyclerView recyclerViewStyles;
    private List<ProductStyle> styleList;
    private Button btnAddStyle, btnSubmitAddProduct, btnCancelAddProduct;
    private TextInputEditText textInputProductName, textInputProductDescription;
    private AutoCompleteTextView textInputProductCategory;
    private RadioGroup radioGroupTax;
    //
    private String productId;
    private List<String> strProductImgNames;
    private byte[] imageBytes;
    private String productImgName;
    private int uploadImgCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_seller_add_product, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnAddStyle = view.findViewById(R.id.btn_add_style);
        btnSubmitAddProduct = view.findViewById(R.id.btn_submit_add_product);
        btnCancelAddProduct = view.findViewById(R.id.btn_cancel_add_product);
        textInputProductName = view.findViewById(R.id.text_input_product_name);
        textInputProductDescription = view.findViewById(R.id.text_input_product_description);
        textInputProductCategory = view.findViewById(R.id.text_input_product_category);
        radioGroupTax = view.findViewById(R.id.radio_group_tax);

        //0. set product category
        ArrayAdapter<CharSequence> productCatAdapter = ArrayAdapter.createFromResource(getContext(), R.array.arr_product_category, android.R.layout.simple_list_item_1);
        textInputProductCategory.setAdapter(productCatAdapter);
        textInputProductCategory.setOnItemClickListener((parent, view2, position, id) -> {});

        //1. set images recycler view with default to add image
        uriUploadProductImgs = new ArrayList<>();
        GridLayoutManager gm = new GridLayoutManager(getContext(), 4);
        recyclerViewProductImgs = view.findViewById(R.id.rv_add_product_img);
        recyclerViewProductImgs.setLayoutManager(gm);
        setProductImagesAdapter(); // imgsList is empty at this point

        //add images when add img in rv clicked
        productImgFilePicker = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), new ActivityResultCallback<List<Uri>>() {
            @Override
            public void onActivityResult(List<Uri> result) {
                uriUploadProductImgs = result;
                setProductImagesAdapter();
            }
        });

        //2. add style
        styleList = new ArrayList<>();
        recyclerViewStyles = view.findViewById(R.id.rv_added_style);
        btnAddStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SellerAddStyleFragment addStyleFragment = SellerAddStyleFragment.newInstance();

                // Get the fragmentManager
                FragmentManager fragmentManager = getParentFragmentManager();

                // Show the addStyleFragment
                addStyleFragment.show(fragmentManager, "Add Style Frag show");

                // set interface listener
//                addStyleFragment.setmListenr(getContext());
//
//                // Customize the appearance of the dialog if needed
//                addStyleFragment.getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//                addStyleFragment.getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            }
        });

        //3. submit added product
        submitAddProduct();

        //4. cancel
        btnCancelAddProduct.setOnClickListener(v->{
            if(getActivity() != null) {
                getActivity().finish();
            }
        });

    }

//    private void checkUserPermission() {
//        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_MEDIA_IMAGES)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(getContext(), new String[]{Manifest.permission.READ_MEDIA_IMAGES},
//                    REQUEST_PERMISSION_CODE);
//            Toast.makeText(getContext(),"permission asked",Toast.LENGTH_SHORT).show();
//        } else {
//            pickProductImages();
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickProductImages();
            } else {
                Toast.makeText(getContext(),"permission denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pickProductImages() {
        productImgFilePicker.launch("image/*");
        if (uriUploadProductImgs != null) {
            uriUploadProductImgs.clear();
        }
    }

    private void setProductImagesAdapter() {
        SellerAddProductImagesAdapter sellerAddProductImagesAdapter = new SellerAddProductImagesAdapter(getActivity(), uriUploadProductImgs);
        //ItemTouchClass
        ItemTouchHelper.Callback callback =
                new ItemMoveCallback(sellerAddProductImagesAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerViewProductImgs);

        recyclerViewProductImgs.setAdapter(sellerAddProductImagesAdapter);
        sellerAddProductImagesAdapter.setmListener(this);
    }

    private void setStylesAdapter() {
        SellerStyleListAdapter sellerStyleListAdapter = new SellerStyleListAdapter(getContext(), styleList);
        //ItemTouchClass
        ItemTouchHelper.Callback callback =
                new ItemMoveCallback(sellerStyleListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerViewStyles);

        recyclerViewStyles.setAdapter(sellerStyleListAdapter);
        sellerStyleListAdapter.setmListener(this);
    }

    private void submitAddProduct() {
        btnSubmitAddProduct.setOnClickListener(v->{

            String productName = textInputProductName.getText().toString();
            String productDescription = textInputProductDescription.getText().toString();
            String productCategory = textInputProductCategory.getText().toString();
            int taxId = radioGroupTax.getCheckedRadioButtonId();

            //check required input
            if (TextUtils.isEmpty(productName)) {
                Toast.makeText(getContext(),
                        "Please enter the product name.", Toast.LENGTH_SHORT).show();
                textInputProductName.setError("Product name is required.");
                textInputProductName.requestFocus();
            } else if (TextUtils.isEmpty(productDescription)) {
                Toast.makeText(getContext(),
                        "Please enter the product description.", Toast.LENGTH_SHORT).show();
                textInputProductDescription.setError("Product description is required.");
                textInputProductDescription.requestFocus();
            } else if (TextUtils.isEmpty(productCategory)) {
                Toast.makeText(getContext(),
                        "Please select the product category.", Toast.LENGTH_SHORT).show();
                textInputProductCategory.setError("Product category is required.");
                textInputProductCategory.requestFocus();
            } else if (taxId == -1) {
                Toast.makeText(getContext(),
                        "Please select the tax.", Toast.LENGTH_SHORT).show();
            } else if (uriUploadProductImgs.size() == 0) {
                Toast.makeText(getContext(),
                        "Please add at least one product image.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),
                        "Request send.", Toast.LENGTH_SHORT).show();
                firebaseInstance = FirebaseDatabase.getInstance();
                firebaseDatabase = firebaseInstance.getReference("Product");  //Product -> productId -> newProduct
                productId = firebaseDatabase.push().getKey();
                //tax define, so far use taxId as the data we save in db

                //compress product images and upload to FireStorage (uriList to stringList)
                compressProductImages();

                //compress style image and upload to FireStorage (uri to string)
                compressStyleImage(styleList);

                //upload product and styles to Realtime DB
                uploadProduct(productName, productCategory,productDescription,
                        taxId, strProductImgNames, styleList);;
            }

        });
    }

    private void uploadProduct(String productName, String category, String productDescription,
                               double tax, List<String> strProductImgNames, List<ProductStyle> productStyles) {

        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();

        Product newProduct = new Product(productName,category,productDescription,
                tax, strProductImgNames, firebaseUser.getUid(), productStyles);

        firebaseDatabase.child(productId).setValue(newProduct).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Product Upload Successfully", Toast.LENGTH_SHORT).show();
                    // Prevent sell go back
                    if(getActivity() != null) {
                        getActivity().finish();
                    }
                } else {
                    Toast.makeText(getContext(), "Product Upload Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //
//        // Convert the Product object to a HashMap using Gson library
//        Gson gson = new Gson();
//        Type type = new TypeToken<HashMap<String, Object>>() {}.getType();
//        HashMap<String, Object> productMap = gson.fromJson(gson.toJson(newProduct), type);
//
////        HashMap<String, Object> imgMap = new HashMap<>();
////        for (int i = 0; i < strProductImgNames.size(); i++) {
////            imgMap.put(String.valueOf(i), strProductImgNames.get(i));
////        }
////        // Update the productMap with the imgMap
////        productMap.put("strProductImgNames", imgMap);
//
//        // Save the productMap to the Realtime Database
//        firebaseDatabase.child(productId).setValue(productMap)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        // Data saved successfully
//                        Toast.makeText(getContext(), "Product Upload Successfully", Toast.LENGTH_SHORT).show();
//                        Intent intent = new Intent(getContext(), TestPageActivity.class);
//                        // Prevent seller go back
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                        finish();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // Failed to save data
//                        Toast.makeText(getContext(), "Product Upload Failed", Toast.LENGTH_SHORT).show();
//                    }
//                });
//

    }

    private void compressStyleImage(List<ProductStyle> styleList) {
        String styleImgName;
        uploadImgCount = 0;
        for (int i = 0; i< styleList.size(); i++) {
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), Uri.parse(styleList.get(i).getStylePic()));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
                imageBytes = stream.toByteArray();
                styleImgName = productId + "_" + styleList.get(i).getStyleName() + ".jpg";
                styleList.get(i).setStylePic(styleImgName);
                //to FireStorage
                uploadImagwsToFireStorage(styleImgName, imageBytes, styleList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private void compressProductImages() {
        strProductImgNames = new ArrayList<>();
        uploadImgCount = 0;
        for (int i = 0; i < uriUploadProductImgs.size(); i++) {
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uriUploadProductImgs.get(i));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
                imageBytes = stream.toByteArray();
                productImgName = productId + "_" + i + ".jpg";
                strProductImgNames.add(productImgName);
                //to FireStorage
                uploadImagwsToFireStorage(productImgName, imageBytes, uriUploadProductImgs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImagwsToFireStorage(String imageName, byte[] imageBytes, List<?> imageList) {
        productImgsReference = FirebaseStorage.getInstance().getReference().child("ProductImages");
        StorageReference imgReference = productImgsReference.child(imageName);
        // I am saving all the product images from different sellers at the same place now
        imgReference.putBytes(imageBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                uploadImgCount++;
                if(uploadImgCount == imageList.size()) {
                    Log.d("upload", "All Images Are Uploaded");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void addNewProductImg() {
//        checkUserPermission();
    }

    @Override
    public void addStyleInputToList(String styleName, Double price, String imgUri, int idx) {
        Log.d("TestStyle", styleName+ " " + price + " " + imgUri + " " + idx);
        ProductStyle newStyle = new ProductStyle(styleName, price, imgUri);

        if (idx == -1) { //Add a new style
            styleList.add(newStyle);
        } else { //update the style
            styleList.get(idx).setStyleName(styleName);
            styleList.get(idx).setStylePrice(price);
            styleList.get(idx).setStylePic(imgUri);
        }

        //set the recyclerview
        if (styleList != null) {
            //recyclerViewStyles.setVisibility(View.VISIBLE);
            LinearLayoutManager lm = new LinearLayoutManager(getContext());
            recyclerViewStyles.setLayoutManager(lm);
            setStylesAdapter();
        }
    }

    @Override
    public void onStyleEdit(int position) {
        ProductStyle editStyle = styleList.get(position);

        SellerAddStyleFragment editStyleFragment = SellerAddStyleFragment.newInstance(
                editStyle.getStyleName(),
                editStyle.getStylePrice(),
                editStyle.getStylePic(),
                position);

        //Get the fragmentManager and start a transaction
        FragmentManager fragmentManager = getParentFragmentManager();

        //Add the addStyleFragment and commit the transaction
        editStyleFragment.show(fragmentManager, "Edit Style Frag show");
        //set interface listener
        editStyleFragment.setmListenr(this);
    }
}