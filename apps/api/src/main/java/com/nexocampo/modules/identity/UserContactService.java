package com.nexocampo.modules.identity;

import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserContactService {

    private final PhoneNumberService phoneNumberService;
    private final UserRepository userRepository;

    public UserContactService(PhoneNumberService phoneNumberService, UserRepository userRepository) {
        this.phoneNumberService = phoneNumberService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ContactResponse get(User user) {
        return toResponse(user);
    }

    @Transactional
    public ContactResponse update(User user, ContactRequest request) {
        user.setPhoneNumber(phoneNumberService.normalizeForSms(
                request.telefone(), request.notificacoesSmsAtivas()));
        user.setSmsNotificationsEnabled(request.notificacoesSmsAtivas());
        return toResponse(userRepository.save(user));
    }

    private ContactResponse toResponse(User user) {
        return new ContactResponse(user.getPhoneNumber(), user.isSmsNotificationsEnabled());
    }

    public record ContactRequest(@Size(max = 40) String telefone, boolean notificacoesSmsAtivas) {}

    public record ContactResponse(String telefone, boolean notificacoesSmsAtivas) {}
}
