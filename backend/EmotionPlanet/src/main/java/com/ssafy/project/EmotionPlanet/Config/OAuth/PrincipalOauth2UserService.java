package com.ssafy.project.EmotionPlanet.Config.OAuth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.*;
import com.ssafy.project.EmotionPlanet.Dao.UserDao;
import com.ssafy.project.EmotionPlanet.Dto.UserDto;
import com.ssafy.project.EmotionPlanet.Dto.UserSecretDto;
import com.ssafy.project.EmotionPlanet.Service.UserService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@Service
public class PrincipalOauth2UserService {

    @Autowired
    UserDao userDao;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    PrincipalOauth2UserService(@Lazy BCryptPasswordEncoder bCryptPasswordEncoder){
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    private final NetHttpTransport transport = new NetHttpTransport();
    private final JsonFactory jsonFactory = new GsonFactory();

    private final String clientId = "172274534251-7a2a6sthcuviratis75u7gu7utbkdp8d.apps.googleusercontent.com";

    public UserDto tokenVerify(String idToken) {

        System.out.println("idToken : " + clientId);

        GoogleIdTokenVerifier gitVerifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setIssuers(Arrays.asList("https://accounts.google.com", "accounts.google.com"))
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken git = null;

        try {
            git = gitVerifier.verify(idToken);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UserDto user = new UserDto();
        if (git == null) {
            System.out.println("Google ID Token is invalid");
        }else {
            GoogleIdToken.Payload payload = git.getPayload();

            // Print user identifier & Get profile information from payload
            String userId = payload.getSubject();
            System.out.println("User ID: " + userId);
            String email = payload.getEmail();
            boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
            String name = (String) payload.get("sub");
            String pictureUrl = (String) payload.get("picture");
            String pw = bCryptPasswordEncoder.encode("security");

            user.setEmail(email);
            user.setNickname(name);
            user.setProfileImg(pictureUrl);
            user.setPw(pw);
        }
        return user;
    }

    public int insertUser(UserDto userDto) {
        if (userDao.duplicateEmail(userDto.getEmail()) == 0) {
            int result = userDao.userRegister(userDto);
            return result;
        } else {
            return 1;
        }
    }


    /// 카카오 로그인
    //토큰발급
    public String getAccessToken (String authorize_code) {
        String access_Token = "";
        String refresh_Token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";

        try {
            URL url = new URL(reqURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //  URL연결은 입출력에 사용 될 수 있고, POST 혹은 PUT 요청을 하려면 setDoOutput을 true로 설정해야함.
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            //	POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=54115c9fc805ecfb96348d18733e6e4a");  //본인이 발급받은 key
            sb.append("&redirect_uri=https://i6e203.p.ssafy.io/login/KaKaoLogin");     // 본인이 설정해 놓은 경로
            sb.append("&code=" + authorize_code);
            bw.write(sb.toString());
            bw.flush();

            //    결과 코드가 200이라면 성공
            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            //    요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            //    Gson 라이브러리에 포함된 클래스로 JSON파싱 객체 생성
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            access_Token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_Token = element.getAsJsonObject().get("refresh_token").getAsString();

            System.out.println("access_token : " + access_Token);
            System.out.println("refresh_token : " + refresh_Token);

            br.close();
            bw.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return access_Token;
    }

    //유저정보조회
    public HashMap<String, Object> getUserInfo (String access_Token) {

        //    요청하는 클라이언트마다 가진 정보가 다를 수 있기에 HashMap타입으로 선언
        HashMap<String, Object> userInfo = new HashMap<String, Object>();
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            //    요청에 필요한 Header에 포함될 내용
            conn.setRequestProperty("Authorization", "Bearer " + access_Token);

            int responseCode = conn.getResponseCode();
            System.out.println("responseCode : " + responseCode);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(result);

            JSONObject kakao_account = (JSONObject) jsonObject.get("kakao_account");
            JSONObject profile = (JSONObject) kakao_account.get("profile");

            String nickname = Long.toString((Long) jsonObject.get("id"));
            String email = (String) kakao_account.get("email");
            String profileImg = (String) profile.get("profile_image_url");
            System.out.println(nickname);
            System.out.println(email);
            userInfo.put("accessToken", access_Token);
            userInfo.put("nickname", nickname);
            userInfo.put("email", email);
            userInfo.put("profileImg", profileImg);

            UserSecretDto userSecretDto = new UserSecretDto();
            userSecretDto.setEmail((String) userInfo.get("email"));
            userSecretDto.setNickname((String) userInfo.get("nickname"));
            userSecretDto.setProfileImg((String) userInfo.get("profile_image_url"));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return userInfo;
    }


}
