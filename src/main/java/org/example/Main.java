package org.example;

import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {

        /**
         * We first require the details of the repository we need to fetch the data for.
         * We need the name of the owner, name of the repository, developer token(in order to make more requests)
         */
        String owner = "autowarefoundation";
        String repo = "autoware";
        String token = "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";


        /**
         * pullRequestUrl - this is the endpoint which we hit in order to get the information about all the pull requests (open and closed).
         * Here we pass the owner and the repo.
         */
        String pullRequestUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/pulls?state=all";

        /**
         * We use HttpClient in order to hit the pullRequestUrl.
         * We pass the token in the request headers.
         */
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(pullRequestUrl);
        request.addHeader("Authorization", "Bearer " + token);



        /**
         * We execute our request and convert the response into a pull request class which has the following members:
         *  1) id
         *  2) pull request number
         *  3) creation date
         *  4) title
         *  We can add more members as and when required.
         */
        HttpResponse response = httpClient.execute(request);
        String jsonResponse = EntityUtils.toString(response.getEntity());
        Gson gson = new Gson();
        PullRequests[] pullRequests = gson.fromJson(jsonResponse, PullRequests[].class);
        ArrayList<PullRequests> prs = new ArrayList<>(Arrays.asList(pullRequests));


        /**
         * It is possible after hitting the endpoint once we don't get all the data as the data might be stored in different pages.
         * The Link Header contains the information about the following pages. So we retrieve this header and extract the url of the next page.
         * We use httpclient to hit this new url and convert the response.
         * This is done until we extract all the pages
         */
        Header linkHeader = response.getFirstHeader("Link");
        if (linkHeader != null) {
            String linkHeaderValue = linkHeader.getValue();
            String nextPageUrl = extractNextPageUrl(linkHeaderValue);
            while (nextPageUrl != null) {
                HttpGet nextPageRequest = new HttpGet(nextPageUrl);
                nextPageRequest.addHeader("Authorization", "Bearer " + token);
                HttpResponse nextPageResponse = httpClient.execute(nextPageRequest);
                String nextPageJsonResponse = EntityUtils.toString(nextPageResponse.getEntity());
                PullRequests[] temp = gson.fromJson(nextPageJsonResponse, PullRequests[].class);

                prs.addAll(new ArrayList<>(Arrays.asList(temp)));
                 // Print the JSON response for the next page
                linkHeader = nextPageResponse.getFirstHeader("Link");
                nextPageUrl = extractNextPageUrl(linkHeader != null ? linkHeader.getValue() : null);
            }
        }


        System.out.println(prs.size());
        for(PullRequests requests:prs)
        {
            requests.setCreated_at(requests.getCreated_at().substring(0,10));
        }


        /**
         * Now we write our data into a csv file.
         */
        CSVWriter csvWriter = new CSVWriter(new FileWriter("pull_requests.csv"));
        String[] header = {"ID", "Title", "Created At", "Number"};
        csvWriter.writeNext(header);

        for (PullRequests pr : prs) {
            String[] row = {pr.id, pr.title, pr.created_at, pr.number /* Add more fields */};
            csvWriter.writeNext(row);
        }

        csvWriter.close();



    }


    /**
     * Helper Function to extract the next page url
     */
    private static String extractNextPageUrl(String linkHeaderValue) {
        if (linkHeaderValue != null) {
            String[] links = linkHeaderValue.split(",");
            for (String link : links) {
                if (link.contains("rel=\"next\"")) {
                    String[] segments = link.split(";");
                    return segments[0].trim().replaceAll("<|>", "");
                }
            }
        }
        return null;
    }
}
