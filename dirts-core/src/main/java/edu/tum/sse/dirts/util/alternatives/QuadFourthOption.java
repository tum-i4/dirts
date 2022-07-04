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

public class QuadFourthOption<First, Second, Third, Fourth> extends QuadAlternative<First, Second, Third, Fourth> {

    private final Fourth fourth;

    public QuadFourthOption(Fourth fourth) {
        this.fourth = fourth;
    }

    @Override
    public boolean isFourthOption() {
        return true;
    }

    @Override
    public Fourth getAsFourthOption() {
        return fourth;
    }
}