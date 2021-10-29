package com.miaoshaproject.service.impl;

import com.miaoshaproject.DataObject.UserDO;
import com.miaoshaproject.DataObject.UserPasswordDO;
import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired(required = false)//?????????!!!!!!!!!
    private UserDOMapper userDOMapper;

    @Autowired(required = false)
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired(required = false)
    private ValidatorImpl validator;

    @Override
    public UserModel getUserById(Integer id) {
        //调用userdomapper获取到对应的用户dataobject
        UserDO userDO=userDOMapper.selectByPrimaryKey(id);
        if(userDO==null){
            return null;
        }
        //???怎么没有提示
        //通过id获取对应用户加密密码信息
        UserPasswordDO userPasswordDO=userPasswordDOMapper.selectByUserId(userDO.getId());

        return convertFromDataObject(userDO,userPasswordDO);
    }

    @Override
    @Transactional//声明事务
    public void register(UserModel userModel) throws BusinessException {
        //校验
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
//        if (StringUtils.isEmpty(userModel.getName())
//                || userModel.getGender() == null
//                || userModel.getAge() == null
//                || StringUtils.isEmpty(userModel.getTelephone())) {
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
//        }
        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }

        //实现model->dataobject方法
        UserDO userDO = convertFromModel(userModel);
        //insertSelective相对于insert方法，不会覆盖掉数据库的默认值
        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号已注册");
        }

//正常来讲执行完userDOMapper.insertSelective(userDO)后，useDo会被分配到一个自增id，可是这里显示的是id=NULL，数据库里
        //用户id可以正常自增分配？？？？
        userModel.setId(userDO.getId());

        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;
    }

    @Override
    public UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException {
        //通过用户手机获取用户信息


        UserDO userDO = userDOMapper.selectByTelphone(telphone);

        if (userDO == null) {
            throw new BusinessException(EmBusinessError.USER_LOOGIN_FAIL);
        }


        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        //发现错误，新注册的用户id和对应密码id对应不上，从数据库中取不到密码！！！，定位到注册接口

        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);
        System.out.println(userModel);
        //比对用户信息内加密的密码是否和传输进来的密码相匹配
        if (!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())) {
            throw new BusinessException(EmBusinessError.USER_LOOGIN_FAIL);
        }

        return userModel;
    }

    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserPasswordDO userPasswordDO=new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }
    private UserDO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }


    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        if(userDO==null){
            return null;
        }
        UserModel userModel=new UserModel();
        BeanUtils.copyProperties(userDO,userModel);
        if(userPasswordDO!=null) {
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }
        return userModel;
    }
}
