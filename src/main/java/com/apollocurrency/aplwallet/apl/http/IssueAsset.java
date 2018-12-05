/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ASSET_NAME;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DECIMALS;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_NAME;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.apache.commons.lang3.StringUtils;

public final class IssueAsset extends CreateTransaction {

    private static class IssueAssetHolder {
        private static final IssueAsset INSTANCE = new IssueAsset();
    }

    public static IssueAsset getInstance() {
        return IssueAssetHolder.INSTANCE;
    }

    private IssueAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "name", "description", "quantityATU", "decimals", "descriptionLength",
                "isSingleton");
    }

    @Override
    protected CreateTransactionRequestData parseRequest(HttpServletRequest req) throws AplException {

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String decimalsValue = Convert.emptyToNull(req.getParameter("decimals"));

        if (name == null) {
            return new CreateTransactionRequestData(MISSING_NAME);
        }

        name = name.trim();
        if (name.length() < Constants.MIN_ASSET_NAME_LENGTH || name.length() > Constants.MAX_ASSET_NAME_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ASSET_NAME_LENGTH);
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return new CreateTransactionRequestData(INCORRECT_ASSET_NAME);
            }
        }

        if (description != null && description.length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ASSET_DESCRIPTION);
        }

        byte decimals = 0;
        if (decimalsValue != null) {
            try {
                decimals = Byte.parseByte(decimalsValue);
                if (decimals < 0 || decimals > 8) {
                    return new CreateTransactionRequestData(INCORRECT_DECIMALS);
                }
            } catch (NumberFormatException e) {
                return new CreateTransactionRequestData(INCORRECT_DECIMALS);
            }
        }

        long quantityATU = ParameterParser.getQuantityATU(req);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantityATU, decimals);
        return new CreateTransactionRequestData(attachment, account);

    }

    @Override
    protected CreateTransactionRequestData parseFeeCalculationRequest(HttpServletRequest req) throws AplException {

        Object isSingletonValue = req.getParameter("isSingleton");
        if (isSingletonValue != null) {
            boolean isSingleton = ParameterParser.getBoolean(req, "isSingleton", true);
            if (isSingleton) {
                int length = ParameterParser.getInt(req, "descriptionLength", 0, Integer.MAX_VALUE, false, -1);
                return new CreateTransactionRequestData(new Attachment.ColoredCoinsAssetIssuance(null, StringUtils.repeat('*', length), 1, (byte)0),
                        null);
            } else {
                return new CreateTransactionRequestData(new Attachment.ColoredCoinsAssetIssuance(null, "", 0, (byte)0),
                        null);
            }
        }
        String description = req.getParameter("description");
        String decimalsValue = Convert.emptyToNull(req.getParameter("decimals"));

        if (description != null && description.length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
            return new CreateTransactionRequestData(INCORRECT_ASSET_DESCRIPTION);
        }
        byte decimals = 0;
        if (decimalsValue != null) {
            try {
                decimals = Byte.parseByte(decimalsValue);
                if (decimals < 0 || decimals > 8) {
                    return new CreateTransactionRequestData(INCORRECT_DECIMALS);
                }
            } catch (NumberFormatException e) {
                return new CreateTransactionRequestData(INCORRECT_DECIMALS);
            }
        }

        long quantityATU = ParameterParser.getQuantityATU(req);
        return new CreateTransactionRequestData(new Attachment.ColoredCoinsAssetIssuance(null, description, quantityATU, decimals), null);
    }
}
