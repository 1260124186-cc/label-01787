package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.entity.Permission;
import com.xiaoan.bookstore.entity.Role;
import com.xiaoan.bookstore.entity.RolePermission;
import com.xiaoan.bookstore.mapper.PermissionMapper;
import com.xiaoan.bookstore.mapper.RoleMapper;
import com.xiaoan.bookstore.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<Role> listRoles() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>().eq(Role::getStatus, 1).orderByAsc(Role::getId)
        );
    }

    public Role getRoleWithPermissions(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) return null;
        role.setPermissions(rolePermissionMapper.selectPermissionsByRoleId(roleId));
        return role;
    }

    public List<Permission> listPermissions() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().eq(Permission::getStatus, 1).orderByAsc(Permission::getSortOrder)
        );
    }

    public List<Permission> getPermissionsByRoleId(Long roleId) {
        if (roleId == null) return Collections.emptyList();
        return rolePermissionMapper.selectPermissionsByRoleId(roleId);
    }

    public List<String> getPermissionCodesByRoleId(Long roleId) {
        return getPermissionsByRoleId(roleId).stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());
    }

    public boolean hasPermission(Long roleId, String permissionCode) {
        List<String> codes = getPermissionCodesByRoleId(roleId);
        return codes.contains(permissionCode);
    }

    public boolean isSuperAdmin(Long roleId) {
        if (roleId == null) return false;
        Role role = roleMapper.selectById(roleId);
        return role != null && "SUPER_ADMIN".equals(role.getCode());
    }

    @Transactional
    public void updateRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        for (Long pid : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
    }
}
