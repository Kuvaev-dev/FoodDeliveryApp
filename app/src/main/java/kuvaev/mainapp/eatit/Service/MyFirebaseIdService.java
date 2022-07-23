package kuvaev.mainapp.eatit.Service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import kuvaev.mainapp.eatit.Common.Common;
import kuvaev.mainapp.eatit.Model.Token;

public class MyFirebaseIdService {
    public void onTokenRefresh(){
        String tokenRefreshed = FirebaseMessaging.getInstance().getToken().toString();
        if(Common.currentUser != null)
             updateTokenToFirebase(tokenRefreshed);
    }

    private void updateTokenToFirebase(String tokenRefreshed){
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");
        Token token = new Token(tokenRefreshed, false);
        // false because token send from client app

        tokens.child(Common.currentUser.getPhone()).setValue(token);
    }
}
