package com.wafflestudio.spring2025.group.service

import com.wafflestudio.spring2025.group.CreateBadGroupNameException
import com.wafflestudio.spring2025.group.GroupCodeConflictException
import com.wafflestudio.spring2025.group.GroupNameConflictException
import com.wafflestudio.spring2025.group.GroupNotFoundException
import com.wafflestudio.spring2025.group.dto.core.GroupDto
import com.wafflestudio.spring2025.group.model.Group
import com.wafflestudio.spring2025.group.repository.GroupRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

@Service
class GroupService(
    private val groupRepository: GroupRepository,
) {
    @Transactional
    fun create(name: String, detail: String?): GroupDto {
        if (groupRepository.existsByName(name)) {
            throw GroupNameConflictException()
        }
        if (name.length < 2) {
            throw CreateBadGroupNameException()
        }
        if (name.all { it.isDigit() }) {
            throw CreateBadGroupNameException()
        }

        val code = generateUniqueCode()
        val group =
            groupRepository.save(
                Group(
                    name = name,
                    code = code,
                    detail = detail ?: "동아리에 대한 설명이 없습니다.",
                ),
            )
        return GroupDto(group)
    }

    @Transactional(readOnly = true)
    fun getById(groupId: Long): GroupDto {
        val group = groupRepository.findByIdOrNull(groupId) ?: throw GroupNotFoundException()
        return GroupDto(group)
    }

    @Transactional(readOnly = true)
    fun getByCode(code: String): GroupDto {
        val group = groupRepository.findByCode(code) ?: throw GroupNotFoundException()
        return GroupDto(group)
    }

    fun list(): List<GroupDto> {
        return groupRepository.findAll().take(5).map { GroupDto(it) }
    }

    @Transactional
    fun update(groupId: Long,
               name: String?,
               detail: String?
    ): GroupDto {
        val group = groupRepository.findByIdOrNull(groupId) ?: throw GroupNotFoundException()

        if (name != null) {
            if (name.length < 2) {
                throw CreateBadGroupNameException()
            }
            if (name.all { it.isDigit() }) {
                throw CreateBadGroupNameException()
            }
            if (group.name != name && groupRepository.existsByName(name)) {
                throw GroupNameConflictException()
            }
            group.name = name
        }

        if (detail != null) {
            group.detail = detail
        }

        val savedGroup = groupRepository.save(group)
        return GroupDto(savedGroup)
    }

    @Transactional
    fun delete(groupId: Long) {
        val group = groupRepository.findByIdOrNull(groupId) ?: throw GroupNotFoundException()
        groupRepository.delete(group)
    }

    @Transactional
    fun regenerateCode(groupId: Long): GroupDto {
        val group = groupRepository.findByIdOrNull(groupId) ?: throw GroupNotFoundException()
        group.code = generateUniqueCode(exclude = group.code)
        val savedGroup = groupRepository.save(group)
        return GroupDto(savedGroup)
    }

    private fun generateUniqueCode(exclude: String? = null): String {
        repeat(20) {
            val code = generateCode()
            if (code != exclude && !groupRepository.existsByCode(code)) {
                return code
            }
        }
        throw GroupCodeConflictException()
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString(6) {
            repeat(6) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }
}
