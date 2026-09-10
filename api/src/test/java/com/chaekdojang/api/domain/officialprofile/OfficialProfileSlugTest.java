package com.chaekdojang.api.domain.officialprofile;

import com.chaekdojang.api.domain.officialprofile.dto.OfficialProfileCreateRequest;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OfficialProfileSlugTest {

    @Autowired OfficialProfileService officialProfileService;
    @Autowired UserRepository users;
    @Autowired TransactionTemplate tx;

    Long adminId;

    @BeforeEach
    void createAdmin() {
        adminId = tx.execute(status -> {
            String suffix = UUID.randomUUID().toString();
            User admin = users.save(User.create("slug-admin-" + suffix + "@test.com",
                    "슬러그관리자" + suffix.substring(0, 8), null));
            admin.setSuperAdmin();
            return admin.getId();
        });
    }

    @Test
    void 한글_이름은_한글_주소로_만들어진다() {
        String name = "시립도서관" + UUID.randomUUID().toString().substring(0, 4);
        var profile = officialProfileService.createProfile(adminId,
                new OfficialProfileCreateRequest(OfficialProfileType.LIBRARY, name, null, null, null));

        assertThat(profile.slug()).isEqualTo(name.toLowerCase());
    }

    @Test
    void 발음_부호가_있는_영문_이름은_부호를_떼고_만들어진다() {
        String suffix = UUID.randomUUID().toString().substring(0, 4);
        var profile = officialProfileService.createProfile(adminId,
                new OfficialProfileCreateRequest(OfficialProfileType.PUBLISHER,
                        "Café Book " + suffix, null, null, null));

        assertThat(profile.slug()).isEqualTo("cafe-book-" + suffix.toLowerCase());
    }

    @Test
    void 같은_이름이면_뒤에_번호를_붙인다() {
        String name = "같은이름출판사" + UUID.randomUUID().toString().substring(0, 4);
        officialProfileService.createProfile(adminId,
                new OfficialProfileCreateRequest(OfficialProfileType.PUBLISHER, name, null, null, null));
        var second = officialProfileService.createProfile(adminId,
                new OfficialProfileCreateRequest(OfficialProfileType.PUBLISHER, name, null, null, null));

        assertThat(second.slug()).isEqualTo(name.toLowerCase() + "-2");
    }
}
