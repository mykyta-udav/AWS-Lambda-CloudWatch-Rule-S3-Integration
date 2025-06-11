package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(
		lambdaName = "uuid_generator",
		roleName = "uuid_generator-role",
		isPublishVersion = true,
		aliasName = "${lambdas_alias_name}",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@RuleEventSource(targetRule = "uuid_trigger")
@DependsOn(
		name = "uuid-storage",
		resourceType = ResourceType.S3_BUCKET
)
@EnvironmentVariable(key = "target_bucket", value = "${target_bucket}")
public class UuidGenerator implements RequestHandler<Object, Map<String, Object>> {

	private final AmazonS3 s3Client;
	private final Gson gson;

	public UuidGenerator() {
		this.s3Client = AmazonS3ClientBuilder.defaultClient();
		this.gson = new Gson();
	}

	public Map<String, Object> handleRequest(Object request, Context context) {
		String bucketName = System.getenv("target_bucket");

		List<String> uuids = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			uuids.add(UUID.randomUUID().toString());
		}

		Map<String, List<String>> data = new HashMap<>();
		data.put("ids", uuids);

		String jsonContent = gson.toJson(data);
		String fileName = Instant.now().toString();

		s3Client.putObject(bucketName, fileName, jsonContent);

		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("statusCode", 200);
		resultMap.put("body", "Successfully generated and stored 10 UUIDs in " + bucketName);
		return resultMap;
	}
}