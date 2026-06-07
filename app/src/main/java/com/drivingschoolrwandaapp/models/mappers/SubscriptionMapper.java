package com.drivingschoolrwandaapp.models.mappers;

import com.drivingschoolrwandaapp.database.entities.SubscriptionPlan;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionEntity;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionWithPlan;
import com.drivingschoolrwandaapp.models.entities.UserSubscription;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    SubscriptionMapper INSTANCE = Mappers.getMapper(SubscriptionMapper.class);

    // Model to Entity
    UserSubscriptionEntity toEntity(UserSubscription userSubscription);
    SubscriptionPlan toEntity(com.drivingschoolrwandaapp.models.entities.SubscriptionPlan subscriptionPlan);
    List<SubscriptionPlan> toEntity(List<com.drivingschoolrwandaapp.models.entities.SubscriptionPlan> subscriptionPlans);


    // Entity to Model
    com.drivingschoolrwandaapp.models.entities.SubscriptionPlan toModel(SubscriptionPlan subscriptionPlan);
    List<com.drivingschoolrwandaapp.models.entities.SubscriptionPlan> toModel(List<SubscriptionPlan> subscriptionPlans);

    // Combined Entity (WithPlan) to Model
    @Mapping(source = "userSubscription.id", target = "id")
    @Mapping(source = "userSubscription.userId", target = "userId")
    @Mapping(source = "userSubscription.subscriptionPlanId", target = "subscriptionPlanId")
    @Mapping(source = "subscriptionPlan", target = "subscriptionPlan")
    UserSubscription toModel(UserSubscriptionWithPlan userSubscriptionWithPlan);

}
