package dev.httpmarco.polocloud.platforms.exceptions

import java.lang.RuntimeException

class DuplicatedPlatformActionException(val id: String) : RuntimeException("A platform action with the same name is already registered! $id") {
}