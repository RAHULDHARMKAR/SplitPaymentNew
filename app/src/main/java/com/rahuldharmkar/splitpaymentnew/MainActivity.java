package com.rahuldharmkar.splitpaymentnew;


import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.view.CardInputWidget;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;




public class MainActivity extends AppCompatActivity {
    private double totalAmount = 80.00; // Example amount
    private EditText cardAmountInput;
    private EditText cashAmountInput;
    private CardInputWidget cardInputWidget;
    private Stripe stripe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize Stripe
        PaymentConfiguration.init(
                getApplicationContext(),
                "pk_live_51PJXqfSFfHUpOVIjMEzKYfxnSMBOg6vmG0BqrCliu9e0nUzPlh5rlwxTP6D7vLLNWf9fPSZuPEJyYn06YGA5wJYo00qpdvFQXw"
        );
        stripe = new Stripe(
                getApplicationContext(),
                PaymentConfiguration.getInstance(getApplicationContext()).getPublishableKey()
        );

        TextView totalAmountView = findViewById(R.id.totalAmount);
        totalAmountView.setText("Rs " + totalAmount);

        cardAmountInput = findViewById(R.id.cardAmountInput);
        cashAmountInput = findViewById(R.id.cashAmountInput);
        cardInputWidget = findViewById(R.id.cardInputWidget);

        Button payButton = findViewById(R.id.payButton);
        payButton.setOnClickListener(v -> {
            double cardAmount = Double.parseDouble(cardAmountInput.getText().toString());
            double cashAmount = Double.parseDouble(cashAmountInput.getText().toString());

            if (cardAmount + cashAmount == totalAmount) {
                processCardPayment(cardAmount, cashAmount);
                // Simulate cash payment processing
                processCashPayment(cashAmount);
            } else {
                Toast.makeText(this, "The total of split payments must equal the total amount.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void processCardPayment(double cardAmount, double cashAmount) {
        PaymentMethodCreateParams params = cardInputWidget.getPaymentMethodCreateParams();
        if (params != null) {
            // Create PaymentIntent on server and retrieve client secret
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://server.com/create-payment-intent";
            Map<String, String> jsonParams = new HashMap<>();
            jsonParams.put("cardAmount", String.valueOf((int) (cardAmount * 100))); // Amount in cents
            jsonParams.put("cashAmount", String.valueOf((int) (cashAmount * 100))); // Amount in cents

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(jsonParams),
                    response -> {
                        String clientSecret = response.optString("clientSecret");
                        String paymentIntentId = response.optString("paymentIntentId");
                        stripe.confirmPayment(this, ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(params, clientSecret));
                        // Update transaction status on confirmation
                        updateTransactionStatus(paymentIntentId);
                    },
                    error -> Toast.makeText(this, "Payment failed: " + error.getMessage(), Toast.LENGTH_LONG).show()
            );

            queue.add(postRequest);
        } else {
            Toast.makeText(this, "Invalid card details.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTransactionStatus(String paymentIntentId) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://server.com/confirm-payment";
        Map<String, String> jsonParams = new HashMap<>();
        jsonParams.put("paymentIntentId", paymentIntentId);

        JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(jsonParams),
                response -> {
                    boolean success = response.optBoolean("success");
                    if (success) {
                        Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Payment confirmation failed.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Payment confirmation failed: " + error.getMessage(), Toast.LENGTH_LONG).show()
        );

        queue.add(postRequest);
    }

    private void processCashPayment(double cashAmount) {
        // Simulate cash payment success
        Toast.makeText(this, "Cash payment of Rs" + cashAmount + " processed.", Toast.LENGTH_SHORT).show();
    }
}
