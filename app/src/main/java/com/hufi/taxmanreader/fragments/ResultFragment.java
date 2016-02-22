package com.hufi.taxmanreader.fragments;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hufi.taxmanreader.R;
import com.hufi.taxmanreader.GroomApplication;
import com.hufi.taxmanreader.model.Order;
import com.hufi.taxmanreader.model.Ticket;
import com.hufi.taxmanreader.realm.RealmProduct;
import com.hufi.taxmanreader.utils.TaxmanUtils;
import com.victor.loading.rotate.RotateLoading;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultFragment extends Fragment {
    private View rootView;
    private String result;

    private boolean manual;

    private FloatingActionButton status;
    private TextView statusText;
    private TextView lastname;
    private TextView firstname;
    private TextView ticket_id;
    private TextView revoked;

    private TextView product_name;
    private TextView product_price;

    private TextView event_name;
    private TextView event_location;

    private CardView product_information;
    private TextView product_label;
    private CardView event_information;
    private TextView event_label;

    public static ResultFragment newInstance(String result, Boolean isManual) {
        final ResultFragment resultFragment = new ResultFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(GroomApplication.getContext().getString(R.string.scanner_result), result);
        arguments.putBoolean(GroomApplication.getContext().getString(R.string.scanner_manual), isManual);
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
        revoked = (TextView) this.rootView.findViewById(R.id.revoked);

        product_name = (TextView) this.rootView.findViewById(R.id.product_name);
        product_price = (TextView) this.rootView.findViewById(R.id.product_price);

        event_name = (TextView) this.rootView.findViewById(R.id.event_name);
        event_location = (TextView) this.rootView.findViewById(R.id.event_location);

        product_information = (CardView) this.rootView.findViewById(R.id.product_information);
        event_information = (CardView) this.rootView.findViewById(R.id.event_information);

        product_label = (TextView) this.rootView.findViewById(R.id.ticket_label);
        event_label = (TextView) this.rootView.findViewById(R.id.event_label);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.scanner_result));

        if(!manual) {
            load();
        } else {
            //@TODO
        }

        return this.rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    private void load() {
        result = getArguments().getString(getString(R.string.scanner_result));

        assert result != null;
        if (!result.isEmpty()) {
            success();
            /*event_loading.start();
            ticket_loading.start();*/
        } else {
            failure(getString(R.string.invalid));
        }
    }

    private void success() {
        Gson gson = new Gson();
        Ticket ticket = gson.fromJson(result, Ticket.class);

        lastname.setText(ticket.getLastname());
        firstname.setText(ticket.getFirstname());
        ticket_id.setText("ID: " + ticket.getTicket_id());

        RealmProduct product = Realm.getInstance(GroomApplication.getContext()).where(RealmProduct.class)
                .equalTo("id", Integer.valueOf(ticket.getPrid()))
                .findFirst();

        if (product != null) {
            product_name.setText(product.getName());
            product_price.setText(String.format("%s€", product.getPrice()));

            if (product.getEvent() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(product.getEvent().getName());
                event_name.setText(product.getEvent().getName());
                event_location.setText(product.getEvent().getPlace().getName());
            }

            // event_loading.stop();
        }

        // ticket_loading.stop();

        if (TaxmanUtils.userConnected()) {
            GroomApplication.service.getOrder(Integer.valueOf(ticket.getOrid())).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(Call<Order> call, Response<Order> response) {
                    Order order = response.body();
                    if (order != null) {
                        if(order.getRevoked()){
                            failure(getString(R.string.revoked));
                        } else {
                            status.setImageDrawable(GroomApplication.getContext().getResources().getDrawable(R.drawable.ic_done));
                            status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GroomApplication.getContext(), R.color.granted)));
                            statusText.setText(getString(R.string.access_granted));
                            statusText.setTextColor(ContextCompat.getColor(GroomApplication.getContext(), R.color.granted));
                        }
                    }
                }

                @Override
                public void onFailure(Call<Order> call, Throwable throwable) {

                }
            });
        } else {
            Toast.makeText(GroomApplication.getContext(), getString(R.string.verif_failed), Toast.LENGTH_SHORT).show();

            status.setImageDrawable(GroomApplication.getContext().getResources().getDrawable(R.drawable.ic_block));
            status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GroomApplication.getContext(), R.color.colorAccent)));
            statusText.setText(getString(R.string.access_unchecked));
            statusText.setTextColor(ContextCompat.getColor(GroomApplication.getContext(), R.color.colorAccent));
        }
    }

    private void failure(String msg) {
        status.setImageDrawable(GroomApplication.getContext().getResources().getDrawable(R.drawable.ic_block));
        status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GroomApplication.getContext(), R.color.denied)));
        statusText.setText(getString(R.string.access_denied));
        statusText.setTextColor(ContextCompat.getColor(GroomApplication.getContext(), R.color.denied));

        revoked.setText(msg);
        revoked.setVisibility(View.VISIBLE);

        hideCards();
    }

    private void hideCards() {
        product_information.setVisibility(View.GONE);
        event_information.setVisibility(View.GONE);
        product_label.setVisibility(View.GONE);
        event_label.setVisibility(View.GONE);
    }
}
