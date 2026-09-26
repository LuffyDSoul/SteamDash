package com.dacs.bff;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dacs.bff.dto.BuildInfoDTO;
import com.dacs.bff.dto.GitInfoDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
public class BffConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BffConfiguration.class);

	final ObjectMapper mapper = new ObjectMapper();
    
    private static final String GIT_PROPERTIES_FILE = "git.properties";

    @Bean(name = "buildInfo")
    public BuildInfoDTO getBuildInfo(@Value("${application.name}") String appName) {
		BuildInfoDTO versionInfo = new BuildInfoDTO();
		versionInfo.setApplicationName(appName);
		try {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(GIT_PROPERTIES_FILE);
			
			GitInfoDTO gitInfo = mapper.readValue(is, GitInfoDTO.class);
			
			versionInfo.setVersion(gitInfo.getBuildVersion());
			versionInfo.setLastBuild(gitInfo.getBuildTime());
			
			String branchWithCommitId = String.format("%s %s",gitInfo.getCommitIdAbbrev(),gitInfo.getBranch());
			versionInfo.setBranchWithCommitId(branchWithCommitId);
			
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return versionInfo;
	}
}