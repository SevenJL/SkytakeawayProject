package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {

        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 对前端传过来的密码 进行MD5加密
        // DigestUtils是用来进行MD5加密的一个方法
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            // 自定义了一个消息常量类 当抛出异常时候 直接调用这些 消息常量类 来输出错误信息
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }


    /**
     * 新增员工
     *
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {

        System.out.println("当前线程的ID:" + Thread.currentThread().getId());


        Employee employee = new Employee();

        // employee.setName(employeeDTO.getName()); 过于繁琐

        // 使用对象属性拷贝
        BeanUtils.copyProperties(employeeDTO, employee); // 前提是属性名一致 可以对应拷贝

        // 前面属性拷贝过来了 需要我们去添加
        // 属性状态 默认状态为正常 1为正常 0为锁定
        // employee.setStatus(1); 这样就为硬编码了 不便于日后维护
        employee.setStatus(StatusConstant.ENABLE); // 直接使用我们自定义规定的 常量类

        // 设置密码
        // password常量类
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 设置当前记录的创建时间 和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        // 设置当前记录的创建人ID和修改人ID

        Long currentId = BaseContext.getCurrentId();
        employee.setCreateUser(currentId);
        employee.setUpdateUser(currentId);


        employeeMapper.insert(employee);


    }

    /**
     * 员工分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize()); // 页码 每页记录数
        // 基于mybatis的一个插件 自动计算

        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal(); // 获取Total
        List<Employee> records = page.getResult(); // 获取 Result

        return new PageResult(total, records);// 传入PageResult
    }

    /**
     * 启用/禁用 员工账号
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 创建实体对象
        // Employee employee = new Employee();
        // employee.setStatus(status);
        // employee.setId(id);
        // 俩种设置属性值的方法
        // @Builder 使用这个注解 可以实现下面的方法
        Employee employee = Employee.builder().status(status).id(id).build();
        employeeMapper.update(employee);
    }
}
