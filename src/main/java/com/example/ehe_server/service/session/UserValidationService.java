package com.example.ehe_server.service.session;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.service.intf.session.UserValidationServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserValidationService implements UserValidationServiceInterface {

    public UserValidationService() {
    }

    @LogMessage(messageKey = "log.message.session.verifyUser")
    @Override
    public void verifyUser() {
    }
}
