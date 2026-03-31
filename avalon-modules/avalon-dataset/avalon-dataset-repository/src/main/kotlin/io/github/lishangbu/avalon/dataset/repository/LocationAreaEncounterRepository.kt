package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.LocationAreaEncounter
import org.babyfish.jimmer.spring.repository.KRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationAreaEncounterRepository : KRepository<LocationAreaEncounter, Long>
