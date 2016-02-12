package com.hufi.taxmanreader.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.hardware.Camera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.hufi.taxmanreader.R;
import com.hufi.taxmanreader.TaxmanReaderApplication;
import com.hufi.taxmanreader.utils.GroomScannerView;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerFragment extends Fragment implements ZXingScannerView.ResultHandler {
  //  private ZXingScannerView mScannerView;

    private GroomScannerView groomScannerView;

    private String cameraIDUsed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        groomScannerView = new GroomScannerView(getActivity());
        groomScannerView.setAutoFocus(true);
       /* mScannerView = new ZXingScannerView(getActivity());
        mScannerView.setAutoFocus(true);*/

        setupFormats();
        if(!setUpBackCamera()) cameraIDUsed = "0";
        //return mScannerView;
        return groomScannerView;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        groomScannerView.setResultHandler(this);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.scanner));
        groomScannerView.startCamera(Integer.valueOf(cameraIDUsed));
    }

    @Override
    public void onPause() {
        super.onPause();
        groomScannerView.stopCamera();
    }

    public void setupFormats() {
        List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>();
        formats.add(BarcodeFormat.QR_CODE);

        if (groomScannerView != null) {
            groomScannerView.setFormats(formats);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.turn_flash:
                if(groomScannerView.getFlash()){
                    groomScannerView.setFlash(false);
                    if(!groomScannerView.getFlash()) item.setIcon(R.drawable.ic_action_flash);
                } else {
                    groomScannerView.setFlash(true);
                    if(groomScannerView.getFlash()) item.setIcon(R.drawable.ic_action_flash_light);
                }
                break;
            case R.id.swap_camera:
                changeCamera();
                groomScannerView.stopCamera();
                groomScannerView.startCamera(Integer.valueOf(cameraIDUsed));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void handleResult(Result result) {
        try {
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decode(getString(R.string.public_key), Base64.DEFAULT));
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(result.getText());
            jws.setKey(publicKey);

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKey(publicKey)
                    .build();

            JwtClaims jwtClaims = jwtConsumer.processToClaims(result.getText());
            launchResult(jwtClaims.getRawJson());
        } catch (InvalidJwtException ex) {
            launchResult("");
        } catch (NoSuchAlgorithmException ex) {
            Toast.makeText(TaxmanReaderApplication.getContext(), getString(R.string.wrong_QR_Code), Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        catch (InvalidKeySpecException ex){
            ex.printStackTrace();
        }
        catch(JoseException ex){
            ex.printStackTrace();
        }
    }



    private boolean changeCamera(){
        int camBackId = Camera.CameraInfo.CAMERA_FACING_BACK;
        int camFrontId = Camera.CameraInfo.CAMERA_FACING_FRONT;

        if(Integer.valueOf(cameraIDUsed) == camBackId){
            cameraIDUsed = String.valueOf(camFrontId);
        } else {
            cameraIDUsed = String.valueOf(camBackId);
        }

        return true;
    }

    private boolean setUpBackCamera(){
        int camBackId = Camera.CameraInfo.CAMERA_FACING_BACK;
        cameraIDUsed = String.valueOf(camBackId);
        return true;
    }

    private void launchResult(String jsonResult){
        ResultFragment fragment = ResultFragment.newInstance(jsonResult, false);
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.scanner_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
