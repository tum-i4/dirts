/*
 * Copyright 2022. The ttrace authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package edu.tum.sse.dirts.util.alternatives;

public abstract class QuadAlternative<First, Second, Third, Fourth> {

    public boolean isFirstOption() {
        return false;
    }

    public boolean isSecondOption() {
        return false;
    }

    public boolean isThirdOption() {
        return false;
    }

    public boolean isFourthOption() {return false;}

    public First getAsFirstOption() {
        throw new UnsupportedOperationException();
    }

    public Second getAsSecondOption() {
        throw new UnsupportedOperationException();
    }

    public Third getAsThirdOption() {
        throw new UnsupportedOperationException();
    }

    public Fourth getAsFourthOption() {
        throw new UnsupportedOperationException();
    }
}
