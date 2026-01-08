package com.wafflestudio.spring2025.group.controller

import com.wafflestudio.spring2025.group.dto.CreateGroupRequest
import com.wafflestudio.spring2025.group.dto.CreateGroupResponse
import com.wafflestudio.spring2025.group.dto.ListGroupResponse
import com.wafflestudio.spring2025.group.dto.UpdateGroupRequest
import com.wafflestudio.spring2025.group.dto.core.GroupDto
import com.wafflestudio.spring2025.group.service.GroupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Group", description = "동아리 관리 API")
class GroupController(
    private val groupService: GroupService,
) {
    @Operation(summary = "동아리 생성", description = "새로운 동아리를 생성합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "동아리 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "409", description = "중복된 동아리 이름"),
        ],
    )
    @PostMapping
    fun create(@RequestBody request: CreateGroupRequest): ResponseEntity<CreateGroupResponse> {
        val group =
            groupService.create(
                name = request.name,
                detail = request.detail,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateGroupResponse(group))
    }

    @Operation(summary = "동아리 목록 조회", description = "모든 동아리 목록을 조회합니다")
    @GetMapping
    fun list(): ResponseEntity<ListGroupResponse> {
        val groups = groupService.list()
        return ResponseEntity.ok(ListGroupResponse(groups))
    }

    @Operation(summary = "동아리 상세 조회", description = "동아리 상세 정보를 조회합니다")
    @GetMapping("/{groupId}")
    fun getById(@PathVariable groupId: Long): ResponseEntity<GroupDto> {
        return ResponseEntity.ok(groupService.getById(groupId))
    }

    @Operation(summary = "동아리 정보 수정", description = "동아리 정보를 수정합니다")
    @PutMapping("/{groupId}")
    fun update(
        @PathVariable groupId: Long,
        @RequestBody request: UpdateGroupRequest,
    ): ResponseEntity<GroupDto> {
        val updatedGroup =
            groupService.update(
                groupId = groupId,
                name = request.name,
                detail = request.detail,
            )
        return ResponseEntity.ok(updatedGroup)
    }

    @Operation(summary = "동아리 삭제", description = "동아리를 삭제합니다")
    @DeleteMapping("/{groupId}")
    fun delete(@PathVariable groupId: Long): ResponseEntity<Unit> {
        groupService.delete(groupId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "초대 코드 재생성", description = "동아리 초대 코드를 재생성합니다")
    @PostMapping("/{groupId}/regenerate-code")
    fun regenerateCode(@PathVariable groupId: Long): ResponseEntity<GroupDto> {
        return ResponseEntity.ok(groupService.regenerateCode(groupId))
    }
}
