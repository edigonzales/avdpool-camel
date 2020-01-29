FROM adoptopenjdk/openjdk11:latest
VOLUME /avdpool_data
EXPOSE 8888

WORKDIR /home/avdpool

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /home/avdpool/app/lib
COPY ${DEPENDENCY}/META-INF /home/avdpool/app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /home/avdpool/app
RUN chown -R 1001:0 /home/avdpool && \
    chmod -R g=u /home/avdpool

USER 1001

ENTRYPOINT ["java","-Xmx2g","-cp","/home/avdpool/app:/home/avdpool/app/lib/*","ch.so.agi.avdpool.AvdpoolApplication"]
