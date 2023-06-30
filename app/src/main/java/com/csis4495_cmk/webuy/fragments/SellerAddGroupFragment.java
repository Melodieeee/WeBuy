package com.csis4495_cmk.webuy.fragments;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.cottacush.android.currencyedittext.CurrencyEditText;
import com.csis4495_cmk.webuy.R;
import com.csis4495_cmk.webuy.activities.MainActivity;
import com.csis4495_cmk.webuy.adapters.SellerAddGroupImagesAdapter;
import com.csis4495_cmk.webuy.adapters.SellerAddGroupStylesAdapter;
import com.csis4495_cmk.webuy.models.Group;
import com.csis4495_cmk.webuy.models.ProductStyle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SellerAddGroupFragment extends Fragment   {

    private NavController navController;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    DatabaseReference groupRef;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser firebaseUser;
    private MaterialButtonToggleGroup tgBtnGp_publish;
    private Button tgBtn_in_stock_publish;
    private Button tgBtn_group_buy_publish;
    private TextInputEditText groupName;
    private TextInputEditText description;

    private TextInputEditText groupPriceRange;
//    private CurrencyEditText groupPriceRange;
    private int groupType;


    private String groupId;


    private String sellerId;
    private int tax;

    private int groupStatus;
    private String productId;
    private RecyclerView rv_img, rv_style;

    private SellerAddGroupStylesAdapter stylesAdapter;

    private SellerAddGroupImagesAdapter imagesAdapter;

    List<ProductStyle> groupStyles;
    List<String> imgPaths;

    Map<String, Integer> groupStylesMap;

    private Button btnStart, btnEnd ;
    private Date startTime, endTime;

    private AppCompatButton btnCancel, btnPublish;

    private AutoCompleteTextView groupCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_seller_add_group, container, false);
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();
        groupRef = firebaseDatabase.getReference("Group");
        //Get passed bundle
        if (getArguments() != null) {
            productId = getArguments().getString("new_group_productId");
        }
        if (firebaseUser != null) {
            sellerId = firebaseUser.getUid();
            // Now you can use the userId as needed
        }

        rv_img = view.findViewById(R.id.rv_groupImg_publish);
        rv_style = view.findViewById(R.id.rv_groupStyle_publish);

        //Load product images and product styles data from database to fragment
        LinearLayoutManager styleLayoutManager = new LinearLayoutManager(getContext());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 4);
        rv_style.setLayoutManager(styleLayoutManager);
        rv_img.setLayoutManager(gridLayoutManager);
//      rv_style.setHasFixedSize(true);
        stylesAdapter = new SellerAddGroupStylesAdapter(getContext());
        rv_style.setAdapter(stylesAdapter);
        imagesAdapter = new SellerAddGroupImagesAdapter();
        rv_img.setAdapter(imagesAdapter);

//        productImageUris = new ArrayList<>();
        groupStyles = new ArrayList<>();
        imgPaths = new ArrayList<>();
        groupStylesMap = new HashMap<>();

        getProdcutData();

        getProductStyle();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        //Set navigation controller, how to navigate simply call -> controller.navigate(destination id)
        navController = NavHostFragment.findNavController(SellerAddGroupFragment.this);

        tgBtnGp_publish = view.findViewById(R.id.tgBtnGp_publish);
        tgBtn_in_stock_publish = view.findViewById(R.id.tgBtn_in_stock_publish);
        tgBtn_group_buy_publish = view.findViewById(R.id.tgBtn_group_buy_publish);

        groupName = view.findViewById(R.id.edit_groupName_publish);
        description = view.findViewById(R.id.edit_groupDes_publish);
        groupPriceRange =view.findViewById(R.id.edit_groupPriceRange_publish);

        btnStart = view.findViewById(R.id.btn_start_group_publish);
        btnEnd = view.findViewById(R.id.btn_end_group_publish);


        //Get group category options
        groupCategory = view.findViewById(R.id.edit_group_category_publish);
        ArrayAdapter<CharSequence> productCatAdapter = ArrayAdapter.createFromResource(getContext(), R.array.arr_product_category, android.R.layout.simple_list_item_1);
        groupCategory.setAdapter(productCatAdapter);
        groupCategory.setOnItemClickListener((parent, view2, position, id) -> {});

        //Set default group type as in stock
        tgBtnGp_publish.check(tgBtn_in_stock_publish.getId());
        tgBtn_in_stock_publish.setClickable(false);
        groupType = 0;
        btnStart.setEnabled(false);
        btnEnd.setEnabled(false);
        //To change group type
        tgBtnGp_publish.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            switch (checkedId){
                case R.id.tgBtn_in_stock_publish:
                    if(isChecked){
                        groupType = 0;
                        startTime = null;
                        endTime = null;
                        Toast.makeText(getContext(), "Group Type "+Integer.toString(groupType) + "Start and End Time: " + startTime + ": " + endTime,Toast.LENGTH_SHORT).show();
                        btnStart.setText("Group Start");
                        btnEnd.setText("Group End");
                        btnStart.setEnabled(false);
                        btnEnd.setEnabled(false);
                        tgBtn_group_buy_publish.setClickable(true);
                        tgBtn_in_stock_publish.setClickable(false);
                    }
                    break;
                case R.id.tgBtn_group_buy_publish:
                    if(isChecked){
                        groupType = 1;
                        startTime = new Date();
                        endTime = new Date();
                        btnStart.setEnabled(true);
                        btnEnd.setEnabled(true);
//                        Toast.makeText(getContext(), "Group Type "+Integer.toString(groupType),Toast.LENGTH_SHORT).show();
                        tgBtn_group_buy_publish.setClickable(false);
                        tgBtn_in_stock_publish.setClickable(true);
                    }
                    break;
                default:
                    groupType = 0;
                    startTime = null;
                    endTime = null;
                    btnStart.setText("Group Start");
                    btnEnd.setText("Group End");
                    btnStart.setEnabled(false);
                    btnEnd.setEnabled(false);
                    tgBtn_group_buy_publish.setClickable(true);
            }
        });

        //Set group startTime and endTime
//        startTime = null;
//        endTime = null;
        btnStart.setOnClickListener(v -> {
            startTime = new Date();
            openDateDialog(startTime, updatedstartTime -> {
                validateStartime();
                btnStart.setText("From " + startTime);
            });
        });
        btnEnd.setOnClickListener(v -> {
            endTime = new Date();
            openDateDialog(endTime, updatedstartTime -> {
                validateEndTime();
                btnEnd.setText("To "+ endTime);
            });
        });

        btnCancel = view.findViewById(R.id.btn_cancel_publish);
        btnPublish = view.findViewById(R.id.btn_publish_publish);

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitNewGroup();
            }
        });

        //Set delete style button listener
        stylesAdapter.setOnImgBtnDeleteStyleListener(new SellerAddGroupStylesAdapter.OnImgBtnDeleteStyleListener() {
            @Override
            public void onDeleteClick(int position) {
                groupStylesMap.remove(groupStyles.get(position).getStyleName());
                groupStyles.remove(position);
                stylesAdapter.updateStyleData2(productId, groupStyles);
                if(groupStyles.size()>0){
                    groupPriceRange.setEnabled(false);
                }else if(groupStyles.size()==0){
                    groupPriceRange.setEnabled(true);
                    groupPriceRange.findFocus();

                }
            }
        });


        //Set edit style price and quantity listener
        stylesAdapter.setOnStyleChangedListner(new SellerAddGroupStylesAdapter.OnStyleChangedListner() {

            @Override
            public void onStyleChange2(int position, ProductStyle style, int qty) {
                groupStylesMap.put(groupStyles.get(position).getStyleName(), qty);
                Toast.makeText(getContext(), groupStyles.get(position).getStyleName() + "qty: " +  groupStylesMap.get(groupStyles.get(position).getStyleName()) , Toast.LENGTH_SHORT ).show();
                groupStyles.set(position, style);
            }

            @Override
            public void onStyleChange(int position, ProductStyle style) {
                String oldKey = groupStyles.get(position).getStyleName();
                Integer qty = groupStylesMap.get(oldKey);
                if(groupStylesMap.containsKey(oldKey)){
                    groupStylesMap.remove(oldKey);
                    groupStylesMap.put(style.getStyleName(), qty);
                }

                groupStyles.set(position, style);

                double minStylePrice = Double.MAX_VALUE;
                double maxStylePrice = Double.MIN_VALUE;

                for (ProductStyle ps: groupStyles) {
                    double stylePrice = ps.getStylePrice();
                    if (stylePrice < minStylePrice) {
                        minStylePrice = stylePrice;
                    }
                    if (stylePrice > maxStylePrice) {
                        maxStylePrice = stylePrice;
                    }
                }

                Log.d("4 Price Range","min "+ minStylePrice + " max: " + maxStylePrice);

                if (minStylePrice == maxStylePrice) {
                    groupPriceRange.setText("CA$ " + minStylePrice);
                    Log.d("5 Price Range","min "+ minStylePrice + " max: " + maxStylePrice);

                } else {
                    groupPriceRange.setText("CA$ " + minStylePrice + "~" + maxStylePrice);
                    Log.d("6 Price Range","min "+ minStylePrice + " max: " + maxStylePrice);

                }


            }
        });

    }

    private void submitNewGroup() {
        Group newGroup = new Group();

        String gName = groupName.getText().toString();
        String gDescription = description.getText().toString();
        String gCategory = groupCategory.getText().toString();
        String gPriceRange = groupPriceRange.getText().toString();

        if (TextUtils.isEmpty(gName)) {
            Toast.makeText(getContext(),
                    "Please enter the group name.", Toast.LENGTH_SHORT).show();
            groupName.setError("Group name is required.");
            groupName.requestFocus();
        }
        if (TextUtils.isEmpty(gDescription)) {
            Toast.makeText(getContext(),
                    "Please enter the group description.", Toast.LENGTH_SHORT).show();
            description.setError("Group description is required.");
            description.requestFocus();
        }

        if (TextUtils.isEmpty(gCategory)) {
            Toast.makeText(getContext(),
                    "Please enter the group category.", Toast.LENGTH_SHORT).show();
            groupCategory.setError("Group category is required.");
            groupCategory.requestFocus();
        }
        if (TextUtils.isEmpty(gPriceRange) && groupStyles.size()==0) {
            Toast.makeText(getContext(),
                    "Please enter the group price.", Toast.LENGTH_SHORT).show();

            groupPriceRange.setError("Group price is required.");
            groupPriceRange.requestFocus();
        }

        newGroup.setSellerId(sellerId);
        newGroup.setProductId(productId);
        newGroup.setGroupName(gName);
        newGroup.setDescription(gDescription);
        newGroup.setCategory(gCategory);
        newGroup.setGroupPrice(gPriceRange);
        newGroup.setGroupType(groupType);
        newGroup.setGroupImages(imgPaths);

        newGroup.setTax(tax);

        if (groupType == 0){
            //group = in stock
            startTime = null;
            endTime = null;
        }else{
            //group = pre order
            if (btnStart.getText().equals("Group Start")){
                    startTime = new Date();
                    openDateDialog(startTime, updatedstartTime -> {
                        validateStartime();
                        btnStart.setText("From " + startTime);
                    });
            }else if(btnEnd.getText().equals("Group End")){
                endTime = new Date();
                openDateDialog(endTime, updatedstartTime -> {
                    validateEndTime();
                    btnEnd.setText("To "+ endTime);
                });
            }
            Date currentTime = new Date();
            if (startTime.compareTo( currentTime) > 0){
                //Group not yet start
                groupStatus = 0;
                newGroup.setStartTime(startTime);
                newGroup.setEndTime(endTime);
                newGroup.setStatus(groupStatus);
            }else if (startTime.compareTo(currentTime) ==0){
                //Group started
                groupStatus = 1;
                newGroup.setStartTime(startTime);
                newGroup.setEndTime(endTime);
                newGroup.setStatus(groupStatus);
            }

        }

        for(Map.Entry<String, Integer> entry : groupStylesMap.entrySet()){
            //check if the style qty is missing
            if ( entry.getValue() == null){
                // trigger alert
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Invalid Quantity");
                builder.setMessage("Please input the quantity for " + entry.getKey());
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;

            }
            if (groupType == 0){
                if ( entry.getValue() < 1){
                    // trigger alert
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Invalid Quantity");
                    builder.setMessage("The quantity for " + entry.getKey() + "must be at least one");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    break;

                }
            }

        }

        for(int i = 0 ; i< stylesAdapter.getItemCount(); i++){
            SellerAddGroupStylesAdapter.ViewHolder viewHolder = (SellerAddGroupStylesAdapter.ViewHolder)rv_style.findViewHolderForAdapterPosition(i);
            if(viewHolder != null && viewHolder.isAnyFieldEmpty()){
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Empty Field");
                builder.setMessage("Please input all the fields");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            }
        }

        if(!groupStylesMap.isEmpty()){
            newGroup.setGroupStylesMap(groupStylesMap);
        }

        if (groupStyles.size()>0){
            newGroup.setGroupStyles(groupStyles);
        }

        if(groupId == null){ //it is a new product
            groupId = groupRef.push().getKey(); //Product -> productId -> newProduct
            Log.d("Test","new group: "+ groupId);
            UploadGroup(newGroup);
        }

    }

    private void UploadGroup(Group group){
        groupRef.child(groupId).setValue(group).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d("Test Upload", "Group Upload Successfully");
                } else {
                    Log.d("Test Upload", "Grooup Upload Failed");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    //Date picker
    private void openDateDialog(Date date, OnDateTimeSetListener listener) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                            (view1, hourOfDay, minute1) -> {
                                Calendar dateTimeCalendar = Calendar.getInstance();
                                dateTimeCalendar.set(year1, monthOfYear, dayOfMonth, hourOfDay, minute1);
                                date.setTime(dateTimeCalendar.getTimeInMillis());
                                if (listener != null) {
                                    listener.onDateTimeSet(date);
                                }
                            }, hour, minute, false);
                    timePickerDialog.show();
                }, year, month, day);
        datePickerDialog.show();
    }

    //Validate end time later than start sime and current time;
    public void validateEndTime(){
        Date currentTime = new Date();
        if (startTime.compareTo(endTime) >= 0 || endTime.compareTo(currentTime) <0  ){
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Invalid End Time!");
            builder.setMessage("Group end time must be after start time and current time");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    openDateDialog(endTime, updatedstartTime -> {
                        btnEnd.setText("To " + endTime);
                        validateEndTime();
                    });
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    //Validate start time later than current time;
    public void validateStartime(){
        Date currentTime = new Date();
        if (startTime.compareTo(currentTime) < 0  ){
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Invalid Start Time!");
            builder.setMessage("Group start time must be after current time");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    openDateDialog(startTime, updatedstartTime -> {
                        btnStart.setText("From " + startTime);
                        validateStartime();
                    });
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    public interface OnDateTimeSetListener {
        void onDateTimeSet(Date dateTime);
    }

    //Get Product data
    private void getProdcutData(){
//        List<String> imgPaths = new ArrayList<>();
        imgPaths.clear();
        DatabaseReference productReference = databaseReference.child("Product").child(productId);
        productReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    DataSnapshot dataSnapshot = task.getResult();
                    if (dataSnapshot.exists()) {
                       String prodcutNameText = dataSnapshot.child("productName").getValue(String.class);
                        String prodcutDesText = dataSnapshot.child("description").getValue(String.class);
                        String prodcutCategoryText = dataSnapshot.child("category").getValue(String.class);
                        String productPriceText = dataSnapshot.child("productPrice").getValue(String.class);
                        tax = dataSnapshot.child("tax").getValue(Integer.class);
                        if (prodcutNameText != null) {
                            groupName.setText(prodcutNameText);
                        }
                        if (prodcutDesText != null) {
                            description.setText(prodcutDesText);
                        }
                        if (prodcutCategoryText != null) {
                            groupCategory.setText(prodcutCategoryText);
                        }
                        if(productPriceText !=null){
                            groupPriceRange.setText(productPriceText);
                        }

                        DataSnapshot productImgSnapshot = dataSnapshot.child("productImages");
                        for (DataSnapshot img : productImgSnapshot.getChildren()) {
                            String imgPath = img.getValue(String.class);
                            imgPaths.add(imgPath);
                        }

                        //Set delete group image button listener
                        if(imgPaths.size() > 1){
                            imagesAdapter.setOnGroupDeleteImgListener(new SellerAddGroupImagesAdapter.onGroupDeleteImgListener() {
                                @Override
                                public void onDeleteClick(int position) {
                                    imgPaths.remove(position);
                                    imagesAdapter.updateGroupImgPaths2(productId,imgPaths);
//                                    Toast.makeText(getContext(), "imgPaths size: " + Integer.toString(imgPaths.size()), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }else{
                            imagesAdapter.setOnGroupDeleteImgListener(new SellerAddGroupImagesAdapter.onGroupDeleteImgListener() {
                                @Override
                                public void onDeleteClick(int position) {

                                }
                            });
                        }
                        imagesAdapter.updateGroupImgPaths2(productId,imgPaths);
                    }
                } else {
                    Log.d(TAG, "Cannot retrieve data from database");
                    Toast.makeText(getContext(), "Cannot retrieve data from database", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //Get Product style(s) data
    private void getProductStyle(){
        databaseReference.child("Product").child(productId).child("productStyles").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                groupStyles.clear();
                groupStylesMap.clear();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    String name = snapshot.child("styleName").getValue(String.class);
                    String img = snapshot.child("stylePic").getValue(String.class);
                    Double price = snapshot.child("stylePrice").getValue(Double.class);
                    ProductStyle ps = new ProductStyle(name, price, img);
                    groupStyles.add(ps);
                    groupStylesMap.put(ps.getStyleName(),null);
                    Toast.makeText(getContext(), ps.getStyleName() + "qty: " +  groupStylesMap.get(ps) , Toast.LENGTH_SHORT ).show();
                }

                if(groupStyles.size()>0){
                    groupPriceRange.setEnabled(false);
                }else if(groupStyles.size()==0){
                    groupPriceRange.setEnabled(true);
                    groupPriceRange.findFocus();
                }

                double minStylePrice = Double.MAX_VALUE;
                double maxStylePrice = Double.MIN_VALUE;

                for (ProductStyle ps: groupStyles) {
                    double stylePrice = ps.getStylePrice();
                    if (stylePrice < minStylePrice) {
                        minStylePrice = stylePrice;
                    }
                    if (stylePrice > maxStylePrice) {
                        maxStylePrice = stylePrice;
                    }
                }

                Log.d("1 Price Range","min "+ minStylePrice + " max: " + maxStylePrice);


                if (minStylePrice == maxStylePrice) {
                    groupPriceRange.setText("CA$ " + minStylePrice);
                    Log.d("2 Price Range","min "+ minStylePrice + " max: " + maxStylePrice);

                } else {
                    groupPriceRange.setText("CA$ " + minStylePrice + "~" + maxStylePrice);
                    Log.d("3 Price Range","min "+ minStylePrice + " max: " + maxStylePrice);

                }


                stylesAdapter.updateStyleData2(productId, groupStyles);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "Cannot retrieve data from database", error.toException());
            }
        });
    }


}