
package ro.androidiasi.codecamp.data.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Track {

    @JsonProperty("name")
    public String name;
    @JsonProperty("capacity")
    public int capacity;
    @JsonProperty("description")
    public String description;
    @JsonProperty("displayOrder")
    public int displayOrder;

}
