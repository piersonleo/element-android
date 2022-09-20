package com.vcard.vchat.mesh.database;

import java.math.BigInteger;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class AccountEntity extends RealmObject {

    @PrimaryKey
    public String address = "";
    public String name = "";
    public String encryptedKey = "";
    public String privateKey = "";

    public String currency = "";
    public Integer nonce = 0;

    public Long balance = 0L;
    public byte[] rootHash = new byte[0];
    public byte[] moduleHash = new byte[0];

    public String type = "";
}
