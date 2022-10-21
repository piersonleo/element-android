package com.vcard.vchat.mesh.database;

import java.math.BigInteger;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AccountMutxoEntity extends RealmObject {

    @PrimaryKey
    public String mutxoKey = "";

    public String fullAddress = "";
    public String currency = "";
    public Long amount = 0L;

    public String source = "";

    public int sourceType = 0;
    public String reference = "";
}
