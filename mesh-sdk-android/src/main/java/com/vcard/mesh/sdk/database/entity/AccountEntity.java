//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.database.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AccountEntity extends RealmObject {

    @PrimaryKey
    public String address = "";
    public String name = "";
    public String encryptedKey = "";
    public String encryptedJson = "";
    public String privateKey = "";

    public String currency = "";
    public String balance = "0";

    public String type = "";
}
