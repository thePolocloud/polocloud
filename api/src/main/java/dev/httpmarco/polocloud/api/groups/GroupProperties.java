/*
 * Copyright 2024 Mirco Lindenau | HttpMarco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.httpmarco.polocloud.api.groups;

import dev.httpmarco.polocloud.api.properties.Property;

import java.io.Serializable;

public class GroupProperties<T> extends Property<T> implements Serializable {

    public GroupProperties(String id, Class<T> type) {
        super(id, type);
    }

    public static GroupProperties<String> TEMPLATES = new GroupProperties<>("templates", String.class);

    public static GroupProperties<Boolean> DEBUG_MODE = new GroupProperties<>("debugMode", Boolean.class);

    public static GroupProperties<Integer> PORT_RANGE = new GroupProperties<>("portRange", int.class);

    public static GroupProperties<Integer> MAX_SERVICES = new GroupProperties<>("maxOnlineServices", int.class);

    public static GroupProperties<Boolean> FALLBACK = new GroupProperties<>("fallback", boolean.class);

    public static GroupProperties<Boolean> STATIC = new GroupProperties<>("static", boolean.class);
}