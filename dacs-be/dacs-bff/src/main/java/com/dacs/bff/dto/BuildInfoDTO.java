package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildInfoDTO {
	String applicationName;
	String version;
	String lastBuild;
	String branchWithCommitId;
	public void setApplicationName(String appName) {
		// TODO Auto-generated method stub
		
	}
}