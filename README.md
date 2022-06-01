# google_ads_standalone

1. Edit the config.properties file found in google_ads_standalone/src/main/resources by replacing the placeholders with valid values

```bash
api.googleads.clientId=ENTER_CLIENT_ID_HERE
api.googleads.clientSecret=ENTER_CLIENT_SECRET_HERE
api.googleads.refreshToken=ENTER_REFRESH_TOKEN_HERE
api.googleads.developerToken=ENTER_DEVELOPER_TOKEN_HERE

googleads.customerId=ENTER_CUSTOMER_ID_HERE
googleads.audienceId=ENTER_AUDIENCE_ID_HERE
```

2. Navigate to the /google_ads_standalone and type in

```bash
mvn clean install
```

3. Once the maven process is done, type in

```bash
docker build -t google_ads_standalone:latest .
```

4. Once the docker build is done, type in

```bash
docker images
```

5. Take note of the image id of google_ads_standalone and run it by typing in

```bash
docker run PUT_IMAGE_ID_HERE
```
