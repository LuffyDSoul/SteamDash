package com.dacs.bff.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dacs.bff.api.client.ApiBackendClient;

@Service
public class ApiBackendServiceImpl implements ApiBackendService{

	@Autowired
	private ApiBackendClient apiBackendClient;

	@Override
	public String ping() {
		return apiBackendClient.ping();
	}
}

