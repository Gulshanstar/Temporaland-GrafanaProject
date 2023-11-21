package helloworldapp;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.apache.http.HttpResponse;
// import io.prometheus.client.Counter;

public class HelloWorldActivitiesImpl implements HelloWorldActivities {

    // Define a Prometheus counter metric
    // private static final Counter activityCounter = Counter.build()
    //         .name("my_java_app_activity_count")
    //         .help("Number of times myActivity is executed")
    //         .register();

    @Override
    public String makeApiCall(String apiUrl) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(apiUrl);

            // Execute the request
            HttpResponse response = httpClient.execute(request);

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);

                // Increment the counter
                // activityCounter.inc();
            }

            return result.toString();
        } catch (IOException e) {
            // Handle exceptions or return an error message
            e.printStackTrace();
            return "Error making API call";


        }

        
    }
   
}