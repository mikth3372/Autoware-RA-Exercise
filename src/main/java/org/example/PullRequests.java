package org.example;

import lombok.Data;

/**
 * Pull Request class with its members
 */

@Data
public class PullRequests {

    String id;

    String number;

    String created_at;

    String title;


}
