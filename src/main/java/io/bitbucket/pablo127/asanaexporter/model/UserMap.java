package io.bitbucket.pablo127.asanaexporter.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class UserMap {
	private List<HashMap<String,String>> data;
}
