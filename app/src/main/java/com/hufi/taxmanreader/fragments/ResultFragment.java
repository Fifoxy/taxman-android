package com.hufi.taxmanreader.fragments;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hufi.taxmanreader.R;
import com.hufi.taxmanreader.TaxmanReaderApplication;
import com.hufi.taxmanreader.async.RequestEventAsyncTask;
import com.hufi.taxmanreader.async.RequestProductAsyncTask;
import com.hufi.taxmanreader.listeners.RequestEventListener;
import com.hufi.taxmanreader.listeners.RequestProductListener;
import com.hufi.taxmanreader.model.Event;
import com.hufi.taxmanreader.model.Product;
import com.hufi.taxmanreader.model.Ticket;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import com.google.gson.Gson;


/**
 * Created by Pierre Defache on 13/12/2015.
 */
public class ResultFragment extends Fragment implements RequestProductListener, RequestEventListener{

    private View rootView;
    private String result;

    private FloatingActionButton status;
    private TextView statusText;
    private TextView lastname;
    private TextView firstname;
    private TextView ticket_id;

    private TextView product_name;
    private TextView product_price;

    private TextView event_name;
    private TextView event_location;

    private CardView product_information;
    private TextView product_label;
    private CardView event_information;
    private TextView event_label;

    public static ResultFragment newInstance(String result) {
        final ResultFragment resultFragment = new ResultFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(TaxmanReaderApplication.getContext().getString(R.string.scanner_result), result);
        resultFragment.setArguments(arguments);
        return resultFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.rootView = inflater.inflate(R.layout.result_fragment, container, false);
        status = (FloatingActionButton) this.rootView.findViewById(R.id.fab);
        statusText = (TextView) this.rootView.findViewById(R.id.status);
        lastname = (TextView) this.rootView.findViewById(R.id.lastname);
        firstname = (TextView) this.rootView.findViewById(R.id.firstname);
        ticket_id = (TextView) this.rootView.findViewById(R.id.ticket_id);

        product_name = (TextView) this.rootView.findViewById(R.id.product_name);
        product_price = (TextView) this.rootView.findViewById(R.id.product_price);

        event_name = (TextView) this.rootView.findViewById(R.id.event_name);
        event_location = (TextView) this.rootView.findViewById(R.id.event_location);

        product_information = (CardView) this.rootView.findViewById(R.id.product_information);
        event_information = (CardView) this.rootView.findViewById(R.id.event_information);

        product_label = (TextView) this.rootView.findViewById(R.id.ticket_label);
        event_label = (TextView) this.rootView.findViewById(R.id.event_label);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.scanner_result));

        load();
        return this.rootView;
    }

    private void load(){
        result = getArguments().getString(getString(R.string.scanner_result));

        assert result != null;
        if(!result.isEmpty()){
            success();
        } else {
            failure();
        }
    }

    private void success(){
        status.setImageDrawable(getActivity().getDrawable(R.drawable.ic_done));
        status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(TaxmanReaderApplication.getContext(), R.color.granted)));
        statusText.setText(getString(R.string.access_granted));
        statusText.setTextColor(ContextCompat.getColor(TaxmanReaderApplication.getContext(), R.color.granted));

        Gson gson = new Gson();
        Ticket ticket = gson.fromJson(result, Ticket.class);

        lastname.setText(ticket.getLastname());
        firstname.setText(ticket.getFirstname());
        ticket_id.setText("ID: " + ticket.getTicket_id());

        RequestProductAsyncTask requestProductAsyncTask = new RequestProductAsyncTask(this);
        requestProductAsyncTask.execute(ticket.getPrid());
    }

    private void failure(){
        status.setImageDrawable(getActivity().getDrawable(R.drawable.ic_block));
        status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(TaxmanReaderApplication.getContext(), R.color.denied)));
        statusText.setText(getString(R.string.access_denied));
        statusText.setTextColor(ContextCompat.getColor(TaxmanReaderApplication.getContext(), R.color.denied));

        hideCards();
    }

    private void hideCards(){
        product_information.setVisibility(View.GONE);
        event_information.setVisibility(View.GONE);
        product_label.setVisibility(View.GONE);
        event_label.setVisibility(View.GONE);
    }

    @Override
    public void onResponseReceived(Event event) {
        if(event != null){
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(event.getName());

            event_name.setText(event.getName());
            event_location.setText(event.getPlace().getName());
        }
    }

    @Override
    public void onResponseReceived(Product product) {
        if(product != null){
            product_name.setText(product.getName());
            product_price.setText(product.getPrice() + "€");

            RequestEventAsyncTask requestEventAsyncTask = new RequestEventAsyncTask(this);
            requestEventAsyncTask.execute(product.getEvent_slug());
        }
    }
}