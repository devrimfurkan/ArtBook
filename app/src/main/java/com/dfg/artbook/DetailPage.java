package com.dfg.artbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public class DetailPage extends AppCompatActivity {

    EditText artNameText, painterNameText, yearText;
    ImageView imageView;
    Button button;
    Bitmap selecetImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_page);

        artNameText = findViewById(R.id.artNameText);
        painterNameText = findViewById(R.id.painterNameText);
        yearText = findViewById(R.id.yearText);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.save);

        //DB oluşturuldu.
        database = openOrCreateDatabase("Arts", MODE_PRIVATE, null);
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        //Eğer info new ile eşleşiyorsa bilgileri getir ve save butonunu görünür yap.
        if (info.matches("new")) {
            artNameText.getText();
            painterNameText.getText();
            yearText.getText();
            button.setVisibility(View.VISIBLE);
            //Galeriden fotoğrafı getir ve resmi uygulamaya koy.
            Bitmap selectedImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.selectimage);
            imageView.setImageBitmap(selectedImage);
        } else {
            //Eğer detailpagein içeriği doluysa sadece butonu görünmez yap.
            int artId = intent.getIntExtra("artId", 1);
            button.setVisibility(View.INVISIBLE);


        try {
            //satırlara artIdleri ata
            Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id=?", new String[]{String.valueOf(artId)});

            int artNameIx=cursor.getColumnIndex("artName");
            int painterNameIx=cursor.getColumnIndex("painterName");
            int yearIx=cursor.getColumnIndex("year");
            int imageIx=cursor.getColumnIndex("image");

            while (cursor.moveToNext()){
                artNameText.setText(cursor.getString(artNameIx));
                painterNameText.setText(cursor.getString(painterNameIx));
                yearText.setText(cursor.getString(yearIx));

                byte[] bytes =cursor.getBlob(imageIx);
                Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                imageView.setImageBitmap(bitmap);
            }cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
    public void selecetImage(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery, 2);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Eğer izin talebinin soncu 1 yani işlem başarılıysa resimi al.
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGalerry = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGalerry, 2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //galiriye erişebiliyoesam ve resimi alabiliyorsam ve de galeriden resmi seçmişsem
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            //remin uri sini getir ve imageDataya ata
            Uri imageData = data.getData();

            try {
                //SDK veriyonum 28 ve üstüyse image kaynak getiricisi oluştur ve imageDatayı ona ata.
                //Bitmap objesinden oluşturulan selectImage'e ata. ve imageViewde göster.
                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageData);
                    selecetImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selecetImage);
                }//SDK versiyonum 28'in altıysa;
                else {
                    selecetImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                    imageView.setImageBitmap(selecetImage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save (View view){
        String artName=artNameText.getText().toString();
        String painterName=painterNameText.getText().toString();
        String year=yearText.getText().toString();

        Bitmap smallImage=makeSmallerImage(selecetImage,300);

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();
        try {
            database= this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artName VARCHAR,painterName VARCHAR,year VARCHAR,image BLOB)");

            String sqlString="INSERT INTO arts(artName,painterName,year,image) VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        Intent intent=new Intent(DetailPage.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }
    public Bitmap makeSmallerImage(Bitmap image,int maxsimumSize){
        int width=image.getWidth();
        int height=image.getHeight();

        float bitmapRatio=(float) width/(float) height;

        if (bitmapRatio>1){
            width=maxsimumSize;
            height=(int)(width/bitmapRatio);
        }else {
            height=maxsimumSize;
            width=(int)(height*bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image,width,height,true);
    }
}