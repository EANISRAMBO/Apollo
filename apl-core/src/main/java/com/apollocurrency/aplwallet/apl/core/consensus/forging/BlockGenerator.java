/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.consensus.forging;

import com.apollocurrency.aplwallet.apl.util.Observable;

public interface BlockGenerator extends Observable<Generator, Event> {
    Generator startGeneration(Generator generator);

    Generator stopGeneration(Generator generator);

    void performGenerationIteration();

    void setGenerationDelay(int delay);

}
