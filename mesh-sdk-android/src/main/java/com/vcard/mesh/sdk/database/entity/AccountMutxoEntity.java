//
// Created by Pierson Leo on 21/11/2022.
// Copyright (c) 2022 vCard Pte Ltd. All rights reserved.
// Use of this source code is governed by the license that can be found in the LICENSE file.
//

package com.vcard.mesh.sdk.database.entity;

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
