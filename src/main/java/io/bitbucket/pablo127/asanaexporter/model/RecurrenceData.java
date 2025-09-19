package io.bitbucket.pablo127.asanaexporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RecurrenceData {
    @JsonProperty("days_of_week")
    private List<Integer> daysOfWeek;

    private Integer frequency;

    @JsonProperty("original_due_date")
    private Long originalDueDateTimestamp;
}
