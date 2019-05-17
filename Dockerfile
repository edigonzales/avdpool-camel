FROM adoptopenjdk/openjdk11:jdk-11.0.2.9
VOLUME /avdpool_data
EXPOSE 8888

ENV USER_NAME avdpool
ENV APP_HOME /home/$USER_NAME/app

RUN useradd -ms /bin/bash $USER_NAME
RUN mkdir $APP_HOME

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
RUN chown -R $USER_NAME /app

USER $USER_NAME

ENTRYPOINT ["java","-cp","app:app/lib/*","ch.so.agi.avdpool.AvdpoolApplication"]
