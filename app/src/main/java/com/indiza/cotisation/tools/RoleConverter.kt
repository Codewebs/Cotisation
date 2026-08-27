package com.indiza.cotisation.tools

import androidx.room.TypeConverter
import com.indiza.cotisation.users.Role

class RoleConverter {

    @TypeConverter
    fun fromRole(role: Role): String {
        return role.name
    }

    @TypeConverter
    fun toRole(roleName: String): Role {
        return Role.valueOf(roleName)
    }
}