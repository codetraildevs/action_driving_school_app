
package com.drivingschoolrwandaapp.data.models;

import com.google.gson.annotations.SerializedName;

public class Pagination {

    @SerializedName("page")
    private int page;

    @SerializedName("limit")
    private int limit;

    @SerializedName("total")
    private int total;

    @SerializedName("pages")
    private int pages;

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public int getTotal() {
        return total;
    }

    public int getPages() {
        return pages;
    }
}
