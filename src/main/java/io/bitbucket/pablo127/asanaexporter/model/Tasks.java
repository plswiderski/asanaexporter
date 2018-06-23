package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Tasks {

    private List<TaskShort> data;

    @JsonProperty("next_page")
    private NextPage nextPage;
}
