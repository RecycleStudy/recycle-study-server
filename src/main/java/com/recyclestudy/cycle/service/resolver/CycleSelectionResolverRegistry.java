package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.selection.CycleSelection;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CycleSelectionResolverRegistry {

    private final Map<Class<? extends CycleSelection>, CycleSelectionResolver<?>> resolvers;

    public CycleSelectionResolverRegistry(final List<CycleSelectionResolver<?>> resolverList) {
        this.resolvers = resolverList.stream()
                .collect(Collectors.toMap(
                        CycleSelectionResolver::getSupportedType,
                        Function.identity()
                ));
    }

    @SuppressWarnings("unchecked")
    public List<Duration> resolve(final CycleSelection selection) {
        final CycleSelectionResolver<CycleSelection> resolver =
                (CycleSelectionResolver<CycleSelection>) resolvers.get(selection.getClass());

        if (resolver == null) {
            throw new IllegalStateException("지원하지 않는 주기 타입입니다: " + selection.getClass().getSimpleName());
        }

        return resolver.resolve(selection);
    }
}
